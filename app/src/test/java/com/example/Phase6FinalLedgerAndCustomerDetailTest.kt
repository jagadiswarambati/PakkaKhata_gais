package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PakkaKhataDatabase
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Money
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome
import com.example.domain.usecase.EvaluateReconciliationUseCase
import com.example.domain.usecase.ExecuteSettlementUseCase
import com.example.presentation.util.DateTimeFormatter
import java.util.Calendar
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
class Phase6FinalLedgerAndCustomerDetailTest {

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

    @Test
    fun testThreeMinuteHackathonDemoFlow_Credit_Partial_FullSettlement() = runBlocking {
        // Step 1: Shopkeeper records credit: "Ramesh took 500 rupees credit"
        val recordResult = repository.recordCreditObligation(
            customerName = "Ramesh Kumar",
            amount = Money.fromRupees(500),
            voiceTranscript = "Ramesh Kumar 500 udhar",
            notes = "General store goods"
        )
        assertTrue("Credit obligation recording should succeed", recordResult.isSuccess)
        val initialObligation = recordResult.getOrThrow()
        assertEquals(50000L, initialObligation.originalAmount.paise)
        assertEquals(50000L, initialObligation.remainingAmount.paise)
        assertEquals(ObligationStatus.OPEN, initialObligation.status)

        // Verify customer ledger balance is ₹500
        val customer = repository.customerRepo.getCustomerByIdDirect(initialObligation.customerId)
        assertNotNull(customer)
        assertEquals(50000L, customer!!.currentBalance.paise)
        assertEquals("₹500", customer.currentBalance.formatRupees())

        // Step 2: Customer pays ₹300 via UPI (PhonePe). OCR captures evidence.
        val paymentEvidence1 = PaymentEvidence(
            imagePath = "/data/evidence/phonepe_300.jpg",
            extractedAmount = Money.fromRupees(300),
            extractedSenderName = "Ramesh Kumar",
            utrNumber = "UPI329019283011",
            ocrRawText = "Paid to Kirana Store ₹300 from Ramesh Kumar UTR UPI329019283011",
            paymentApp = "PhonePe"
        )
        val evidenceId1 = repository.evidenceRepo.saveEvidence(paymentEvidence1)
        assertTrue(evidenceId1 > 0)

        // Step 3: Match & Settle Partial Payment (₹300)
        val matchEval1 = evaluateReconciliationUseCase(evidenceId1).getOrThrow()
        val bestMatch1 = matchEval1.evaluation.bestMatch
        assertNotNull("Should find match for Ramesh Kumar", bestMatch1)
        assertEquals(initialObligation.id, bestMatch1!!.obligation.id)

        val settlementResult1 = executeSettlementUseCase(
            evidenceId = evidenceId1,
            obligationId = initialObligation.id,
            confidence = bestMatch1.confidence
        ).getOrThrow()

        assertEquals(SettlementOutcome.PARTIALLY_SETTLED, settlementResult1.outcome)
        assertEquals(20000L, settlementResult1.remainingAmount.paise)
        assertEquals("₹200", settlementResult1.remainingAmount.formatRupees())

        // Verify Customer balance is now ₹200
        val customerAfterP1 = repository.customerRepo.getCustomerByIdDirect(initialObligation.customerId)
        assertEquals(20000L, customerAfterP1!!.currentBalance.paise)

        // Verify customer reconciliation query returns 1 settlement
        val custReconciliationsP1 = repository.reconciliationRepo.getReconciliationsByCustomerIdDirect(customer.id)
        assertEquals(1, custReconciliationsP1.size)
        assertEquals(30000L, custReconciliationsP1[0].settledAmount.paise)

        // Step 4: Customer pays remaining ₹200 via GPay. OCR captures evidence.
        val paymentEvidence2 = PaymentEvidence(
            imagePath = "/data/evidence/gpay_200.jpg",
            extractedAmount = Money.fromRupees(200),
            extractedSenderName = "Ramesh K",
            utrNumber = "UPI482910482910",
            ocrRawText = "Completed ₹200 to Kirana Store from Ramesh K UTR UPI482910482910",
            paymentApp = "Google Pay"
        )
        val evidenceId2 = repository.evidenceRepo.saveEvidence(paymentEvidence2)
        assertTrue(evidenceId2 > 0)

        // Step 5: Match & Settle Second Payment (₹200)
        val matchEval2 = evaluateReconciliationUseCase(evidenceId2).getOrThrow()
        val bestMatch2 = matchEval2.evaluation.bestMatch
        assertNotNull(bestMatch2)

        val settlementResult2 = executeSettlementUseCase(
            evidenceId = evidenceId2,
            obligationId = initialObligation.id,
            confidence = bestMatch2!!.confidence
        ).getOrThrow()

        assertEquals(SettlementOutcome.FULLY_SETTLED, settlementResult2.outcome)
        assertEquals(0L, settlementResult2.remainingAmount.paise)
        assertEquals("₹0", settlementResult2.remainingAmount.formatRupees())

        // Verify Obligation is FULLY_SETTLED
        val finalObligation = repository.obligationRepo.getObligationByIdDirect(initialObligation.id)
        assertNotNull(finalObligation)
        assertEquals(ObligationStatus.FULLY_SETTLED, finalObligation!!.status)
        assertEquals(0L, finalObligation.remainingAmount.paise)

        // Verify Customer balance is now ₹0
        val finalCustomer = repository.customerRepo.getCustomerByIdDirect(initialObligation.customerId)
        assertNotNull(finalCustomer)
        assertEquals(0L, finalCustomer!!.currentBalance.paise)

        // Verify Customer History shows all 2 linked settlements
        val allCustomerReconciliations = repository.reconciliationRepo.getReconciliationsByCustomerIdDirect(customer.id)
        assertEquals(2, allCustomerReconciliations.size)
        val totalSettledPaise = allCustomerReconciliations.sumOf { it.settledAmount.paise }
        assertEquals(50000L, totalSettledPaise)

        // Verify Payment Evidences are both marked reconciled
        val ev1 = repository.evidenceRepo.getEvidenceByIdDirect(evidenceId1)
        val ev2 = repository.evidenceRepo.getEvidenceByIdDirect(evidenceId2)
        assertTrue("Evidence 1 must be marked reconciled", ev1!!.isReconciled)
        assertTrue("Evidence 2 must be marked reconciled", ev2!!.isReconciled)

        // Verify unreconciled list is now empty
        val unreconciled = repository.evidenceRepo.getUnreconciledEvidenceDirect()
        assertTrue("Unreconciled list should be empty", unreconciled.isEmpty())
    }

    @Test
    fun testManualRecordPaymentFlow_500to300to200toZero() = runBlocking {
        // Obligation of ₹500
        val recordResult = repository.recordCreditObligation(
            customerName = "Jagadiswar",
            amount = Money.fromRupees(500),
            voiceTranscript = "Jagadiswar 500 credit"
        )
        assertTrue(recordResult.isSuccess)
        val obligation = recordResult.getOrThrow()

        // 1. First manual payment of ₹300
        val payment1 = Money.fromRupees(300)
        val evidence1 = PaymentEvidence(
            imagePath = "",
            extractedAmount = payment1,
            extractedSenderName = "Jagadiswar",
            paymentApp = "Cash",
            ocrRawText = "Direct Payment: Cash"
        )
        val plan1 = com.example.domain.reconciliation.SettlementCalculator.calculate(
            remainingAmount = obligation.remainingAmount,
            paidAmount = payment1
        )
        val settle1 = repository.executeAtomicSettlement(
            evidence = evidence1,
            obligationId = obligation.id,
            settledAmount = plan1.settledAmount,
            matchConfidence = 1.0f,
            outcome = plan1.outcome
        )
        assertTrue(settle1.isSuccess)

        val obAfterP1 = repository.obligationRepo.getObligationByIdDirect(obligation.id)
        assertNotNull(obAfterP1)
        assertEquals(Money.fromRupees(200), obAfterP1!!.remainingAmount)
        assertEquals(ObligationStatus.PARTIALLY_SETTLED, obAfterP1.status)

        val custAfterP1 = repository.customerRepo.getCustomerByIdDirect(obligation.customerId)
        assertEquals(Money.fromRupees(200), custAfterP1!!.currentBalance)

        // 2. Second manual payment of remaining ₹200
        val payment2 = Money.fromRupees(200)
        val evidence2 = PaymentEvidence(
            imagePath = "",
            extractedAmount = payment2,
            extractedSenderName = "Jagadiswar",
            paymentApp = "UPI",
            ocrRawText = "Direct Payment: UPI"
        )
        val plan2 = com.example.domain.reconciliation.SettlementCalculator.calculate(
            remainingAmount = obAfterP1.remainingAmount,
            paidAmount = payment2
        )
        val settle2 = repository.executeAtomicSettlement(
            evidence = evidence2,
            obligationId = obligation.id,
            settledAmount = plan2.settledAmount,
            matchConfidence = 1.0f,
            outcome = plan2.outcome
        )
        assertTrue(settle2.isSuccess)

        val obAfterP2 = repository.obligationRepo.getObligationByIdDirect(obligation.id)
        assertNotNull(obAfterP2)
        assertEquals(Money.ZERO, obAfterP2!!.remainingAmount)
        assertEquals(ObligationStatus.FULLY_SETTLED, obAfterP2.status)

        val custAfterP2 = repository.customerRepo.getCustomerByIdDirect(obligation.customerId)
        assertEquals(Money.ZERO, custAfterP2!!.currentBalance)
    }

    @Test
    fun testDateTimeFormatter() {
        val now = System.currentTimeMillis()
        val formattedToday = DateTimeFormatter.formatRelativeTime(now)
        assertTrue("Timestamp from today should contain 'Today'", formattedToday.contains("Today"))

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val formattedYesterday = DateTimeFormatter.formatRelativeTime(cal.timeInMillis)
        assertTrue("Timestamp from yesterday should contain 'Yesterday'", formattedYesterday.contains("Yesterday"))

        val formattedZero = DateTimeFormatter.formatRelativeTime(0L)
        assertEquals("Just now", formattedZero)
    }

    @Test
    fun testMoneyFormattingIndianStyle() {
        assertEquals("₹500", Money.fromRupees(500).formatRupees())
        assertEquals("₹1,250", Money.fromRupees(1250).formatRupees())
        assertEquals("₹12,500", Money.fromRupees(12500).formatRupees())
        assertEquals("₹0", Money.ZERO.formatRupees())
        assertEquals("₹50.50", Money.fromPaise(5050L).formatRupees())
        assertEquals("-₹500", Money.fromPaise(-50000L).formatRupees())
    }
}
