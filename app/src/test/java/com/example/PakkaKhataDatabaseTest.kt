package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PakkaKhataDatabase
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ObligationEntity
import com.example.data.local.entities.PaymentEvidenceEntity
import com.example.data.local.entities.ReconciliationEntity
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PakkaKhataDatabaseTest {

    private lateinit var database: PakkaKhataDatabase
    private lateinit var ledgerRepository: LedgerRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PakkaKhataDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ledgerRepository = LedgerRepositoryImpl(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testMoneyPaiseCalculations() {
        val amount1 = Money.fromRupees(500L) // 50,000 paise
        val amount2 = Money.fromRupees(300L) // 30,000 paise
        val difference = amount1 - amount2

        assertEquals(50000L, amount1.paise)
        assertEquals(30000L, amount2.paise)
        assertEquals(20000L, difference.paise)
        assertEquals("₹200", difference.formatRupees())

        val partialPaise = Money.fromPaise(25050L) // ₹250.50
        assertEquals("₹250.50", partialPaise.formatRupees())
    }

    @Test
    fun testCustomerAndObligationCreation() = runBlocking {
        // 1. Create Customer
        val customerId = database.customerDao().insertCustomer(
            CustomerEntity(
                name = "Ramesh Kumar",
                normalizedName = "ramesh kumar",
                phoneNumber = "9876543210",
                currentBalancePaise = 50000L
            )
        )
        assertTrue(customerId > 0)

        // 2. Create Obligation for customer (₹500)
        val obligationId = database.obligationDao().insertObligation(
            ObligationEntity(
                customerId = customerId,
                originalAmountPaise = 50000L,
                remainingAmountPaise = 50000L,
                voiceTranscript = "Ramesh ko paanch sau udhar",
                notes = "Kirana goods",
                status = ObligationStatus.OPEN
            )
        )
        assertTrue(obligationId > 0)

        val fetchedObligations = database.obligationDao().getObligationsByCustomer(customerId).first()
        assertEquals(1, fetchedObligations.size)
        assertEquals(50000L, fetchedObligations[0].originalAmountPaise)
        assertEquals(ObligationStatus.OPEN, fetchedObligations[0].status)
    }

    @Test
    fun testAtomicSettlementTransaction() = runBlocking {
        // Setup customer with ₹500 credit
        val customerId = ledgerRepository.customerRepo.saveCustomer(
            Customer(
                name = "Ramesh",
                normalizedName = "ramesh",
                currentBalance = Money.fromRupees(500L)
            )
        )

        val obligationId = ledgerRepository.obligationRepo.createObligation(
            Obligation(
                customerId = customerId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                voiceTranscript = "Ramesh 500 credit"
            )
        )

        // Simulate incoming payment evidence of ₹300
        val evidence = PaymentEvidence(
            imagePath = "/storage/evidence_sample.jpg",
            extractedAmount = Money.fromRupees(300L),
            extractedSenderName = "Ramesh Kumar",
            utrNumber = "429104829102",
            ocrRawText = "Paid ₹300 to Shopkeeper UTR 429104829102",
            paymentApp = "PhonePe"
        )

        // Execute atomic settlement
        val result = ledgerRepository.executeAtomicSettlement(
            evidence = evidence,
            obligationId = obligationId,
            settledAmount = Money.fromRupees(300L),
            matchConfidence = 0.95f,
            outcome = SettlementOutcome.PARTIALLY_SETTLED
        ).getOrThrow()

        assertEquals(Money.fromRupees(300L), result.settledAmount)
        assertEquals(Money.fromRupees(200L), result.remainingAmount)
        assertEquals(SettlementOutcome.PARTIALLY_SETTLED, result.outcome)

        // Verify Obligation state in DB
        val updatedObligation = ledgerRepository.obligationRepo.getObligationByIdDirect(obligationId)
        assertNotNull(updatedObligation)
        assertEquals(Money.fromRupees(200L), updatedObligation!!.remainingAmount)
        assertEquals(ObligationStatus.PARTIALLY_SETTLED, updatedObligation.status)

        // Verify Customer Balance in DB
        val updatedCustomer = ledgerRepository.customerRepo.getCustomerByIdDirect(customerId)
        assertNotNull(updatedCustomer)
        assertEquals(Money.fromRupees(200L), updatedCustomer!!.currentBalance)

        // Verify Reconciliation link in DB
        val reconciliations = ledgerRepository.reconciliationRepo.getReconciliationsByObligation(obligationId).first()
        assertEquals(1, reconciliations.size)
        assertEquals(Money.fromRupees(300L), reconciliations[0].settledAmount)
    }
}
