package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PakkaKhataDatabase
import com.example.data.perception.PaymentImageManager
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Money
import com.example.domain.model.PaymentEvidence
import com.example.domain.perception.OCRProvider
import com.example.domain.perception.OcrExtractionResult
import com.example.presentation.screens.evidence.CaptureUiPhase
import com.example.presentation.screens.evidence.PaymentCaptureViewModel
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentEvidenceDatabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var database: PakkaKhataDatabase
    private lateinit var repository: LedgerRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        dbFile = context.getDatabasePath("test_evidence_khata.db")
        if (dbFile.exists()) dbFile.delete()

        database = Room.databaseBuilder(context, PakkaKhataDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
        if (dbFile.exists()) dbFile.delete()
    }

    @Test
    fun testPaymentEvidence_saveAndRetrieveDirect() {
        runBlocking {
            val evidence = PaymentEvidence(
                imagePath = "/data/user/0/com.example/files/payment_evidence/evidence_1.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Ramesh Kumar",
                utrNumber = "4291823910",
                ocrRawText = "Payment Successful\n₹500\nPaid by Ramesh Kumar\nUPI Ref: 4291823910",
                paymentApp = "PhonePe"
            )

            val id = repository.evidenceRepo.saveEvidence(evidence)
            assertTrue(id > 0)

            val retrieved = repository.evidenceRepo.getEvidenceByIdDirect(id)
            assertNotNull(retrieved)
            assertEquals(50000L, retrieved!!.extractedAmount.paise)
            assertEquals("Ramesh Kumar", retrieved.extractedSenderName)
            assertEquals("4291823910", retrieved.utrNumber)
            assertEquals("PhonePe", retrieved.paymentApp)
        }
    }

    @Test
    fun testPaymentEvidence_retrieveByUtr() {
        runBlocking {
            val evidence1 = PaymentEvidence(
                imagePath = "/data/img1.jpg",
                extractedAmount = Money.fromRupees(300L),
                extractedSenderName = "Suresh",
                utrNumber = "123456789012"
            )
            val evidence2 = PaymentEvidence(
                imagePath = "/data/img2.jpg",
                extractedAmount = Money.fromRupees(450L),
                extractedSenderName = "Mahesh",
                utrNumber = "998877665544"
            )

            repository.evidenceRepo.saveEvidence(evidence1)
            repository.evidenceRepo.saveEvidence(evidence2)

            val found = repository.evidenceRepo.getEvidenceByUtr("123456789012")
            assertNotNull(found)
            assertEquals(Money.fromRupees(300L), found!!.extractedAmount)
            assertEquals("Suresh", found.extractedSenderName)

            val notFound = repository.evidenceRepo.getEvidenceByUtr("000000000000")
            assertNull(notFound)
        }
    }

    @Test
    fun testPaymentEvidence_preservesFractionalPaisePrecision() {
        runBlocking {
            val fractionalMoney = Money.fromPaise(35075L) // ₹350.75
            val evidence = PaymentEvidence(
                imagePath = "/data/fractional.jpg",
                extractedAmount = fractionalMoney,
                extractedSenderName = "Anita"
            )

            val id = repository.evidenceRepo.saveEvidence(evidence)
            val retrieved = repository.evidenceRepo.getEvidenceByIdDirect(id)
            assertNotNull(retrieved)
            assertEquals(35075L, retrieved!!.extractedAmount.paise)
        }
    }

    @Test
    fun testPaymentEvidence_flowUpdatesOnInsertAndDelete() {
        runBlocking {
            val evidenceListBefore = repository.evidenceRepo.getAllEvidence().first()
            assertEquals(0, evidenceListBefore.size)

            val evidence = PaymentEvidence(
                imagePath = "/data/test.jpg",
                extractedAmount = Money.fromRupees(200L)
            )
            val id = repository.evidenceRepo.saveEvidence(evidence)

            val evidenceListAfter = repository.evidenceRepo.getAllEvidence().first()
            assertEquals(1, evidenceListAfter.size)
            assertEquals(id, evidenceListAfter[0].id)

            repository.evidenceRepo.deleteEvidence(evidenceListAfter[0])
            val evidenceListDeleted = repository.evidenceRepo.getAllEvidence().first()
            assertEquals(0, evidenceListDeleted.size)
        }
    }

    @Test
    fun testPaymentCaptureViewModel_saveEvidenceFlow() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val mockOcrProvider = object : OCRProvider {
                override val isAvailable: Boolean = true
                override suspend fun processImage(imagePath: String): Result<OcrExtractionResult> {
                    return Result.success(
                        OcrExtractionResult(
                            rawText = "Payment Successful ₹750 Paid by Anita Ref: 88776655",
                            extractedAmountPaise = 75000L,
                            senderName = "Anita",
                            utrNumber = "88776655",
                            paymentApp = "Google Pay",
                            confidence = 0.9f
                        )
                    )
                }
            }

            val testFile = File(context.filesDir, "test_evidence.jpg").apply {
                createNewFile()
                writeText("dummy image bytes")
            }

            val viewModel = PaymentCaptureViewModel(
                application = app,
                ledgerRepository = repository,
                ocrProvider = mockOcrProvider,
                imageManager = PaymentImageManager(app)
            )

            // Capture image triggers OCR
            viewModel.onImageCaptured(testFile)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()

            val stateAfterOcr = viewModel.uiState.value
            assertEquals(CaptureUiPhase.REVIEW, stateAfterOcr.phase)
            assertEquals("750", stateAfterOcr.extractedDetails.amountRupees)
            assertEquals("Anita", stateAfterOcr.extractedDetails.senderName)
            assertEquals("88776655", stateAfterOcr.extractedDetails.utrNumber)
            assertEquals("Google Pay", stateAfterOcr.extractedDetails.paymentApp)

            // Save evidence
            viewModel.savePaymentEvidence()
            var retries = 50
            while (viewModel.uiState.value.phase != CaptureUiPhase.SAVED_READY && retries > 0) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                Thread.sleep(20)
                retries--
            }

            val stateAfterSave = viewModel.uiState.value
            assertEquals(CaptureUiPhase.SAVED_READY, stateAfterSave.phase)
            assertNotNull(stateAfterSave.savedEvidenceId)

            // Verify entity in database
            val savedInDb = repository.evidenceRepo.getEvidenceByIdDirect(stateAfterSave.savedEvidenceId!!)
            assertNotNull(savedInDb)
            assertEquals(75000L, savedInDb!!.extractedAmount.paise)
            assertEquals("Anita", savedInDb.extractedSenderName)
            assertEquals("88776655", savedInDb.utrNumber)
            assertEquals("Google Pay", savedInDb.paymentApp)

            testFile.delete()
        }
    }
}
