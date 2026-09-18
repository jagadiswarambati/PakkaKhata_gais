package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PakkaKhataDatabase
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome
import com.example.domain.reconciliation.MatchDecision
import com.example.domain.usecase.EvaluateReconciliationUseCase
import com.example.domain.usecase.ExecuteSettlementUseCase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReconciliationAndSettlementIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: PakkaKhataDatabase
    private lateinit var repository: LedgerRepositoryImpl
    private lateinit var evaluateReconciliationUseCase: EvaluateReconciliationUseCase
    private lateinit var executeSettlementUseCase: ExecuteSettlementUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PakkaKhataDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(database)
        evaluateReconciliationUseCase = EvaluateReconciliationUseCase(repository)
        executeSettlementUseCase = ExecuteSettlementUseCase(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 1. Exact name + exact amount -> auto match (settles obligation)
    @Test
    fun testScenario1_exactNameAndAmount_autoMatch() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(Customer(name = "Ramesh Kumar"))
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img1.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Ramesh Kumar",
                utrNumber = "111122223333"
            )
        )

        val evalResult = evaluateReconciliationUseCase(evId)
        assertTrue(evalResult.isSuccess)
        val reviewData = evalResult.getOrThrow()

        assertEquals(MatchDecision.AUTO_MATCH, reviewData.evaluation.decision)
        assertEquals(obId, reviewData.evaluation.bestMatch?.obligation?.id)
        assertTrue((reviewData.evaluation.bestMatch?.confidence ?: 0f) >= 0.85f)

        // Settle
        val settleResult = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId,
            confidence = reviewData.evaluation.bestMatch?.confidence ?: 1.0f
        )
        assertTrue(settleResult.isSuccess)

        val updatedOb = repository.obligationRepo.getObligationByIdDirect(obId)
        assertNotNull(updatedOb)
        assertEquals(Money.ZERO, updatedOb!!.remainingAmount)
        assertEquals(ObligationStatus.FULLY_SETTLED, updatedOb.status)
    }

    // 2. Fuzzy name match + exact amount -> suggested match with review
    @Test
    fun testScenario2_fuzzyNameAndExactAmount_suggestedMatch() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(Customer(name = "Ramesh Kumar"))
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )
        // Slightly different spelling "Ramesh Kumaar"
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img2.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Ramesh Kumaar",
                utrNumber = "222233334444"
            )
        )

        val evalResult = evaluateReconciliationUseCase(evId)
        assertTrue(evalResult.isSuccess)
        val reviewData = evalResult.getOrThrow()

        // Should be SUGGESTED_MATCH or AUTO_MATCH with review
        assertTrue(
            reviewData.evaluation.decision == MatchDecision.SUGGESTED_MATCH ||
            reviewData.evaluation.decision == MatchDecision.AUTO_MATCH
        )
        assertEquals(obId, reviewData.evaluation.bestMatch?.obligation?.id)
    }

    // 3. Same amount, different customer -> suggested match or low confidence review
    @Test
    fun testScenario3_sameAmountDifferentCustomer_suggestedOrNoMatch() = runBlocking {
        val custId1 = repository.customerRepo.saveCustomer(Customer(name = "Ramesh Kumar"))
        val custId2 = repository.customerRepo.saveCustomer(Customer(name = "Suresh Patel"))

        repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId1,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )
        val obId2 = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId2,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )

        // Payment from Suresh
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img3.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Suresh Patel",
                utrNumber = "333344445555"
            )
        )

        val evalResult = evaluateReconciliationUseCase(evId)
        assertTrue(evalResult.isSuccess)
        val reviewData = evalResult.getOrThrow()

        // Best match should be Suresh's obligation, not Ramesh's
        assertEquals(obId2, reviewData.evaluation.bestMatch?.obligation?.id)
    }

    // 4. Exact amount, unknown customer -> manual assignment required
    @Test
    fun testScenario4_exactAmountUnknownCustomer_manualAssignmentRequired() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(Customer(name = "Ramesh Kumar"))
        repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )

        // Payment with null / unknown sender name
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img4.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = null,
                utrNumber = "444455556666"
            )
        )

        val evalResult = evaluateReconciliationUseCase(evId)
        assertTrue(evalResult.isSuccess)
        val reviewData = evalResult.getOrThrow()

        // Without sender name, decision cannot be confident AUTO_MATCH
        assertTrue(
            reviewData.evaluation.decision == MatchDecision.SUGGESTED_MATCH ||
            reviewData.evaluation.decision == MatchDecision.NO_MATCH
        )
    }

    // 5. Partial payment (₹300 on ₹500) -> remaining amount becomes ₹200, status PARTIALLY_SETTLED
    @Test
    fun testScenario5_partialPayment_partiallySettled() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(
            Customer(name = "Amit Sharma", currentBalance = Money.fromRupees(500L))
        )
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img5.jpg",
                extractedAmount = Money.fromRupees(300L),
                extractedSenderName = "Amit Sharma",
                utrNumber = "555566667777"
            )
        )

        val settleResult = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId,
            confidence = 1.0f
        )
        assertTrue(settleResult.isSuccess)

        val updatedOb = repository.obligationRepo.getObligationByIdDirect(obId)
        assertNotNull(updatedOb)
        assertEquals(Money.fromRupees(200L), updatedOb!!.remainingAmount)
        assertEquals(ObligationStatus.PARTIALLY_SETTLED, updatedOb.status)

        // Customer balance updated from 500 to 200
        val updatedCustomer = repository.customerRepo.getCustomerByIdDirect(custId)
        assertNotNull(updatedCustomer)
        assertEquals(Money.fromRupees(200L), updatedCustomer!!.currentBalance)
    }

    // 6. Full payment (₹500 on ₹500) -> remaining amount becomes ₹0, status FULLY_SETTLED
    @Test
    fun testScenario6_fullPayment_fullySettled() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(
            Customer(name = "Vikram Singh", currentBalance = Money.fromRupees(500L))
        )
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img6.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Vikram Singh",
                utrNumber = "666677778888"
            )
        )

        val settleResult = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId,
            confidence = 1.0f
        )
        assertTrue(settleResult.isSuccess)

        val updatedOb = repository.obligationRepo.getObligationByIdDirect(obId)
        assertNotNull(updatedOb)
        assertEquals(Money.ZERO, updatedOb!!.remainingAmount)
        assertEquals(ObligationStatus.FULLY_SETTLED, updatedOb.status)

        // Customer balance becomes 0
        val updatedCust = repository.customerRepo.getCustomerByIdDirect(custId)
        assertEquals(Money.ZERO, updatedCust!!.currentBalance)
    }

    // 7. Overpayment (₹600 on ₹500) -> handled as defined by domain logic
    @Test
    fun testScenario7_overpayment_handledCorrectly() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(
            Customer(name = "Neha Gupta", currentBalance = Money.fromRupees(500L))
        )
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img7.jpg",
                extractedAmount = Money.fromRupees(600L),
                extractedSenderName = "Neha Gupta",
                utrNumber = "777788889999"
            )
        )

        val settleResult = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId,
            confidence = 1.0f
        )
        assertTrue(settleResult.isSuccess)

        val updatedOb = repository.obligationRepo.getObligationByIdDirect(obId)
        assertNotNull(updatedOb)
        assertEquals(ObligationStatus.OVERPAID, updatedOb!!.status)
        assertEquals(Money.ZERO, updatedOb.remainingAmount)

        // Reconciliation record reflects settled amount (500) and excess tracked
        val recon = repository.reconciliationRepo.getReconciliationByIdDirect(settleResult.getOrThrow().reconciliationId)
        assertNotNull(recon)
        assertEquals(Money.fromRupees(500L), recon!!.settledAmount)
        assertEquals(SettlementOutcome.OVERPAID, recon.matchType)
    }

    // 8. Duplicate payment detection -> warning shown, duplicate settlement blocked
    @Test
    fun testScenario8_duplicatePaymentDetection() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(Customer(name = "Deepak Verma"))
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(400L),
                remainingAmount = Money.fromRupees(400L),
                status = ObligationStatus.OPEN
            )
        )
        val utr = "DUPLICATE_UTR_123"
        val evId1 = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img8_1.jpg",
                extractedAmount = Money.fromRupees(400L),
                extractedSenderName = "Deepak Verma",
                utrNumber = utr
            )
        )

        // Settle first payment
        executeSettlementUseCase(
            evidenceId = evId1,
            obligationId = obId,
            confidence = 1.0f
        )

        // Capture duplicate payment evidence with identical UTR
        val evId2 = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img8_2.jpg",
                extractedAmount = Money.fromRupees(400L),
                extractedSenderName = "Deepak Verma",
                utrNumber = utr
            )
        )

        val evalResult = evaluateReconciliationUseCase(evId2)
        assertTrue(evalResult.isSuccess)
        val reviewData = evalResult.getOrThrow()

        // Must detect duplicate
        assertTrue(reviewData.isDuplicate)
        assertNotNull(reviewData.duplicateWarning)
        assertTrue(reviewData.duplicateWarning!!.contains("duplicate", ignoreCase = true))
    }

    // 9. Manual customer reassignment -> updates matched obligation correctly
    @Test
    fun testScenario9_manualCustomerReassignment() = runBlocking {
        val custId1 = repository.customerRepo.saveCustomer(Customer(name = "Customer A"))
        val custId2 = repository.customerRepo.saveCustomer(Customer(name = "Customer B"))

        val obId1 = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId1,
                originalAmount = Money.fromRupees(250L),
                remainingAmount = Money.fromRupees(250L),
                status = ObligationStatus.OPEN
            )
        )
        val obId2 = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId2,
                originalAmount = Money.fromRupees(250L),
                remainingAmount = Money.fromRupees(250L),
                status = ObligationStatus.OPEN
            )
        )

        // Evidence with ambiguous or mismatched sender
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img9.jpg",
                extractedAmount = Money.fromRupees(250L),
                extractedSenderName = "Customer A",
                utrNumber = "999900001111"
            )
        )

        // Shopkeeper manually assigns payment to Customer B's obligation obId2
        val settleResult = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId2,
            confidence = 1.0f
        )
        assertTrue(settleResult.isSuccess)

        // Customer B obligation settled
        val updatedOb2 = repository.obligationRepo.getObligationByIdDirect(obId2)
        assertEquals(ObligationStatus.FULLY_SETTLED, updatedOb2!!.status)

        // Customer A obligation remains untouched and OPEN
        val updatedOb1 = repository.obligationRepo.getObligationByIdDirect(obId1)
        assertEquals(ObligationStatus.OPEN, updatedOb1!!.status)
        assertEquals(Money.fromRupees(250L), updatedOb1.remainingAmount)
    }

    // 10. Settle action -> atomically writes Obligation update, Evidence update, and Reconciliation record
    @Test
    fun testScenario10_atomicSettlement_writesAllThreeRecords() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(
            Customer(name = "Pooja Roy", currentBalance = Money.fromRupees(800L))
        )
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(800L),
                remainingAmount = Money.fromRupees(800L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img10.jpg",
                extractedAmount = Money.fromRupees(800L),
                extractedSenderName = "Pooja Roy",
                utrNumber = "101010101010"
            )
        )

        val settleResult = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId,
            confidence = 0.95f
        )
        assertTrue(settleResult.isSuccess)
        val reconId = settleResult.getOrThrow().reconciliationId

        // Verify Obligation updated
        val updatedOb = repository.obligationRepo.getObligationByIdDirect(obId)
        assertEquals(ObligationStatus.FULLY_SETTLED, updatedOb!!.status)
        assertEquals(Money.ZERO, updatedOb.remainingAmount)

        // Verify Evidence marked reconciled
        val updatedEv = repository.evidenceRepo.getEvidenceByIdDirect(evId)
        assertTrue(updatedEv!!.isReconciled)

        // Verify Reconciliation record saved
        val recon = repository.reconciliationRepo.getReconciliationByIdDirect(reconId)
        assertNotNull(recon)
        assertEquals(obId, recon!!.obligationId)
        assertEquals(evId, recon.evidenceId)
        assertEquals(Money.fromRupees(800L), recon.settledAmount)
        assertEquals(SettlementOutcome.FULLY_SETTLED, recon.matchType)
    }

    // 11. Reconciled evidence cannot be settled a second time
    @Test
    fun testScenario11_reconciledEvidenceCannotBeSettledAgain() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(Customer(name = "Aakash"))
        val obId1 = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(350L),
                remainingAmount = Money.fromRupees(350L),
                status = ObligationStatus.OPEN
            )
        )
        val obId2 = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(350L),
                remainingAmount = Money.fromRupees(350L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img11.jpg",
                extractedAmount = Money.fromRupees(350L),
                extractedSenderName = "Aakash",
                utrNumber = "111111111111"
            )
        )

        // Settle once
        val firstSettle = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId1,
            confidence = 1.0f
        )
        assertTrue("firstSettle should succeed: " + firstSettle.exceptionOrNull(), firstSettle.isSuccess)

        // Attempt second settlement with the same evidence
        val secondSettle = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId2,
            confidence = 1.0f
        )
        assertTrue("secondSettle should fail: " + secondSettle.getOrNull(), secondSettle.isFailure)
        assertTrue(
            "secondSettle message was: " + secondSettle.exceptionOrNull()?.message,
            secondSettle.exceptionOrNull()?.message?.contains("already been reconciled", ignoreCase = true) == true
        )
    }

    // 12. Database state integrity verified via Room integration tests
    @Test
    fun testScenario12_databaseIntegrity() = runBlocking {
        val custId = repository.customerRepo.saveCustomer(Customer(name = "Manoj"))
        val obId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(1000L),
                remainingAmount = Money.fromRupees(1000L),
                status = ObligationStatus.OPEN
            )
        )
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img12.jpg",
                extractedAmount = Money.fromRupees(400L),
                extractedSenderName = "Manoj",
                utrNumber = "121212121212"
            )
        )

        val unreconciledBefore = repository.evidenceRepo.getUnreconciledEvidenceDirect()
        assertTrue(unreconciledBefore.any { it.id == evId })

        executeSettlementUseCase(
            evidenceId = evId,
            obligationId = obId,
            confidence = 1.0f
        )

        val unreconciledAfter = repository.evidenceRepo.getUnreconciledEvidenceDirect()
        assertFalse(unreconciledAfter.any { it.id == evId })

        val reconList = repository.reconciliationRepo.getReconciliationsByEvidenceDirect(evId)
        assertEquals(1, reconList.size)
        assertEquals(Money.fromRupees(400L), reconList[0].settledAmount)
    }

    // 13. Repository settlement transaction rolls back on failure
    @Test
    fun testScenario13_settlementTransactionRollbackOnInvalidObligation() = runBlocking {
        val evId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/img13.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Test Rollback",
                utrNumber = "131313131313"
            )
        )

        val nonExistentObligationId = 999999L
        val result = executeSettlementUseCase(
            evidenceId = evId,
            obligationId = nonExistentObligationId,
            confidence = 1.0f
        )

        assertTrue(result.isFailure)

        // Evidence should NOT be reconciled
        val ev = repository.evidenceRepo.getEvidenceByIdDirect(evId)
        assertFalse(ev!!.isReconciled)

        // No reconciliation record written
        val allRecons = repository.reconciliationRepo.getAllReconciliationsDirect()
        assertTrue(allRecons.none { it.evidenceId == evId })
    }

    // 14. Voice-created obligation settles correctly with screenshot-captured payment evidence
    @Test
    fun testScenario14_voiceCreatedObligationSettlesWithScreenshotEvidence() = runBlocking {
        // Step A: Voice credit creation (simulated parse: "Rohan ko paanch sau rupaye udhar diye")
        val custId = repository.customerRepo.saveCustomer(
            Customer(name = "Rohan", currentBalance = Money.fromRupees(500L))
        )
        val voiceObligationId = repository.obligationRepo.createObligation(
            Obligation(
                customerId = custId,
                originalAmount = Money.fromRupees(500L),
                remainingAmount = Money.fromRupees(500L),
                status = ObligationStatus.OPEN,
                voiceTranscript = "Rohan ko 500 rupaye udhaar diye",
                notes = "Voice credit"
            )
        )

        // Step B: Screenshot capture & OCR extraction simulation
        val screenshotEvidenceId = repository.evidenceRepo.saveEvidence(
            PaymentEvidence(
                imagePath = "/data/user/0/com.example/files/payment_evidence/evidence_rohan.jpg",
                extractedAmount = Money.fromRupees(500L),
                extractedSenderName = "Rohan",
                utrNumber = "UPI/2026/09/18/ROHAN500",
                ocrRawText = "Payment Received\n₹500\nFrom: Rohan\nUPI Reference: UPI/2026/09/18/ROHAN500",
                paymentApp = "Google Pay"
            )
        )

        // Step C: Evaluate reconciliation
        val evalResult = evaluateReconciliationUseCase(screenshotEvidenceId)
        assertTrue(evalResult.isSuccess)
        val reviewData = evalResult.getOrThrow()

        assertEquals(MatchDecision.AUTO_MATCH, reviewData.evaluation.decision)
        assertEquals(voiceObligationId, reviewData.evaluation.bestMatch?.obligation?.id)

        // Step D: Execute atomic settlement
        val settleResult = executeSettlementUseCase(
            evidenceId = screenshotEvidenceId,
            obligationId = voiceObligationId,
            confidence = reviewData.evaluation.bestMatch?.confidence ?: 1.0f
        )
        assertTrue(settleResult.isSuccess)

        // Step E: Verify full end-to-end ledger state
        val settledObligation = repository.obligationRepo.getObligationByIdDirect(voiceObligationId)
        assertNotNull(settledObligation)
        assertEquals(Money.ZERO, settledObligation!!.remainingAmount)
        assertEquals(ObligationStatus.FULLY_SETTLED, settledObligation.status)

        val updatedCustomer = repository.customerRepo.getCustomerByIdDirect(custId)
        assertNotNull(updatedCustomer)
        assertEquals(Money.ZERO, updatedCustomer!!.currentBalance)

        val reconciledEvidence = repository.evidenceRepo.getEvidenceByIdDirect(screenshotEvidenceId)
        assertNotNull(reconciledEvidence)
        assertTrue(reconciledEvidence!!.isReconciled)
    }
}
