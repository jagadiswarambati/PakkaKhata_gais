package com.example

import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome
import com.example.domain.reconciliation.DefaultReconciliationEngine
import com.example.domain.reconciliation.FuzzyMatcher
import com.example.domain.reconciliation.MatchDecision
import com.example.domain.reconciliation.NameNormalizer
import com.example.domain.reconciliation.ReconciliationConfig
import com.example.domain.reconciliation.SettlementCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReconciliationEngineTest {

    private lateinit var engine: DefaultReconciliationEngine

    @Before
    fun setup() {
        engine = DefaultReconciliationEngine()
    }

    // ==========================================
    // MATCHING TESTS (1 - 7)
    // ==========================================

    @Test
    fun test01_exactCustomerNameMatch() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val obligation = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )
        val evidence = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
        assertNotNull(evaluation.bestMatch)
        assertEquals(1.0f, evaluation.bestMatch!!.nameScore, 0.01f)
        assertEquals(1.0f, evaluation.bestMatch!!.overallScore, 0.01f)
    }

    @Test
    fun test02_caseInsensitiveMatch() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val obligation = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )
        val evidenceUpper = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "RAMESH"
        )
        val evidenceLower = evidenceUpper.copy(extractedSenderName = "ramesh")

        val evalUpper = engine.evaluate(evidenceUpper, listOf(customer), listOf(obligation))
        val evalLower = engine.evaluate(evidenceLower, listOf(customer), listOf(obligation))

        assertEquals(MatchDecision.AUTO_MATCH, evalUpper.decision)
        assertEquals(MatchDecision.AUTO_MATCH, evalLower.decision)
        assertEquals(1.0f, evalUpper.bestMatch!!.nameScore, 0.01f)
        assertEquals(1.0f, evalLower.bestMatch!!.nameScore, 0.01f)
    }

    @Test
    fun test03_nameAndSurnameVariation() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val obligation = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )
        // Sender has full name "Ramesh Kumar"
        val evidence = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh Kumar"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertNotNull(evaluation.bestMatch)
        assertTrue(evaluation.bestMatch!!.nameScore >= 0.85f)
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
    }

    @Test
    fun test04_tokenContainment() {
        val customer = Customer(id = 1, name = "Ramesh K", normalizedName = "ramesh k")
        val obligation = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )
        // Sender has dotted initial "Ramesh K."
        val evidence = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh K."
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertNotNull(evaluation.bestMatch)
        assertTrue(evaluation.bestMatch!!.nameScore >= 0.90f)
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
    }

    @Test
    fun test05_fuzzyNameMatch() {
        // Typo in OCR or UPI handle: "Ramsh" vs "Ramesh"
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val obligation = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )
        val evidence = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramsh"
        )

        val sim = FuzzyMatcher.calculateLevenshteinSimilarity("ramsh", "ramesh")
        assertTrue(sim >= 0.80f)

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertNotNull(evaluation.bestMatch)
        // Fuzzy match produces a high or suggested score
        assertTrue(evaluation.bestMatch!!.overallScore >= 0.75f)
        assertTrue(evaluation.decision in listOf(MatchDecision.AUTO_MATCH, MatchDecision.SUGGESTED_MATCH))
    }

    @Test
    fun test06_completelyDifferentCustomer() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val obligation = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )
        // Sender is completely different stranger "Unknown Person"
        val evidence = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Unknown Person"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertEquals(MatchDecision.NO_MATCH, evaluation.decision)
        assertNull(evaluation.bestMatch)
    }

    @Test
    fun test07_multipleCandidateCustomers() {
        val cust1 = Customer(id = 1, name = "Ramesh Kumar", normalizedName = "ramesh kumar")
        val cust2 = Customer(id = 2, name = "Suresh Sharma", normalizedName = "suresh sharma")
        val cust3 = Customer(id = 3, name = "Mahesh Verma", normalizedName = "mahesh verma")

        val ob1 = Obligation(id = 1, customerId = 1, originalAmount = Money.fromRupees(500L), remainingAmount = Money.fromRupees(500L))
        val ob2 = Obligation(id = 2, customerId = 2, originalAmount = Money.fromRupees(500L), remainingAmount = Money.fromRupees(500L))
        val ob3 = Obligation(id = 3, customerId = 3, originalAmount = Money.fromRupees(500L), remainingAmount = Money.fromRupees(500L))

        val evidence = PaymentEvidence(
            imagePath = "/evidence/1.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh Kumar"
        )

        val evaluation = engine.evaluate(evidence, listOf(cust1, cust2, cust3), listOf(ob1, ob2, ob3))
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
        assertEquals(1L, evaluation.bestMatch!!.customer.id)
        assertEquals(100, (evaluation.bestMatch!!.overallScore * 100).toInt())
    }

    // ==========================================
    // SETTLEMENT TESTS (8 - 12)
    // ==========================================

    @Test
    fun test08_exactPaymentFullySettled() {
        val remaining = Money.fromRupees(500L)
        val paid = Money.fromRupees(500L)

        val plan = SettlementCalculator.calculate(remaining, paid)
        assertEquals(SettlementOutcome.FULLY_SETTLED, plan.outcome)
        assertEquals(Money.fromRupees(500L), plan.settledAmount)
        assertEquals(Money.ZERO, plan.newRemainingAmount)
        assertEquals(Money.ZERO, plan.excessAmount)
    }

    @Test
    fun test09_partialPaymentPartiallySettled() {
        val remaining = Money.fromRupees(500L)
        val paid = Money.fromRupees(300L)

        val plan = SettlementCalculator.calculate(remaining, paid)
        assertEquals(SettlementOutcome.PARTIALLY_SETTLED, plan.outcome)
        assertEquals(Money.fromRupees(300L), plan.settledAmount)
        assertEquals(Money.fromRupees(200L), plan.newRemainingAmount)
        assertEquals(Money.ZERO, plan.excessAmount)
    }

    @Test
    fun test10_overpaymentOverpaid() {
        val remaining = Money.fromRupees(500L)
        val paid = Money.fromRupees(600L)

        val plan = SettlementCalculator.calculate(remaining, paid)
        assertEquals(SettlementOutcome.OVERPAID, plan.outcome)
        assertEquals(Money.fromRupees(500L), plan.settledAmount)
        assertEquals(Money.ZERO, plan.newRemainingAmount)
        assertEquals(Money.fromRupees(100L), plan.excessAmount)
    }

    @Test
    fun test11_zeroOrInvalidPaymentHandling() {
        val remaining = Money.fromRupees(500L)
        val zeroPayment = Money.ZERO
        val negativePayment = Money.fromPaise(-5000L)

        val planZero = SettlementCalculator.calculate(remaining, zeroPayment)
        assertEquals(SettlementOutcome.NO_MATCH, planZero.outcome)
        assertEquals(Money.ZERO, planZero.settledAmount)
        assertEquals(remaining, planZero.newRemainingAmount)

        val planNegative = SettlementCalculator.calculate(remaining, negativePayment)
        assertEquals(SettlementOutcome.NO_MATCH, planNegative.outcome)
        assertEquals(Money.ZERO, planNegative.settledAmount)
        assertEquals(remaining, planNegative.newRemainingAmount)
    }

    @Test
    fun test12_multipleSequentialPayments() {
        // Original debt = ₹500
        val originalAmount = Money.fromRupees(500L)

        // Payment 1 = ₹300
        val payment1 = Money.fromRupees(300L)
        val plan1 = SettlementCalculator.calculate(originalAmount, payment1)
        assertEquals(SettlementOutcome.PARTIALLY_SETTLED, plan1.outcome)
        assertEquals(Money.fromRupees(300L), plan1.settledAmount)
        assertEquals(Money.fromRupees(200L), plan1.newRemainingAmount)

        // Payment 2 = ₹200 applied to the remaining balance from Payment 1
        val payment2 = Money.fromRupees(200L)
        val plan2 = SettlementCalculator.calculate(plan1.newRemainingAmount, payment2)
        assertEquals(SettlementOutcome.FULLY_SETTLED, plan2.outcome)
        assertEquals(Money.fromRupees(200L), plan2.settledAmount)
        assertEquals(Money.ZERO, plan2.newRemainingAmount)
    }

    // ==========================================
    // DECISION TESTS (13 - 16)
    // ==========================================

    @Test
    fun test13_highConfidenceCandidateAutoMatch() {
        val customer = Customer(id = 1, name = "Sunil Verma", normalizedName = "sunil verma")
        val obligation = Obligation(
            id = 10,
            customerId = 1,
            originalAmount = Money.fromRupees(1200L),
            remainingAmount = Money.fromRupees(1200L)
        )
        val evidence = PaymentEvidence(
            imagePath = "/ev/1.png",
            extractedAmount = Money.fromRupees(1200L),
            extractedSenderName = "Sunil Verma"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
        assertTrue(evaluation.bestMatch!!.overallScore >= 0.90f)
        assertEquals(SettlementOutcome.FULLY_SETTLED, evaluation.bestMatch!!.settlementPlan.outcome)
    }

    @Test
    fun test14_mediumConfidenceCandidateSuggestedMatch() {
        // Compatible partial amount and partial name variation: "Sunil" vs "Sunil K. Verma"
        val customer = Customer(id = 1, name = "Sunil K Verma", normalizedName = "sunil k verma")
        val obligation = Obligation(
            id = 10,
            customerId = 1,
            originalAmount = Money.fromRupees(1000L),
            remainingAmount = Money.fromRupees(1000L)
        )
        val evidence = PaymentEvidence(
            imagePath = "/ev/1.png",
            extractedAmount = Money.fromRupees(400L), // Partial payment
            extractedSenderName = "Sunil"             // Short name
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertNotNull(evaluation.bestMatch)
        assertTrue(
            evaluation.decision == MatchDecision.SUGGESTED_MATCH ||
                    evaluation.decision == MatchDecision.AUTO_MATCH
        )
        assertTrue(evaluation.bestMatch!!.overallScore >= 0.50f)
        assertEquals(SettlementOutcome.PARTIALLY_SETTLED, evaluation.bestMatch!!.settlementPlan.outcome)
    }

    @Test
    fun test15_lowConfidenceCandidateNoMatch() {
        val customer = Customer(id = 1, name = "Harish Patel", normalizedName = "harish patel")
        val obligation = Obligation(
            id = 10,
            customerId = 1,
            originalAmount = Money.fromRupees(1000L),
            remainingAmount = Money.fromRupees(1000L)
        )
        val evidence = PaymentEvidence(
            imagePath = "/ev/1.png",
            extractedAmount = Money.fromRupees(1000L),
            extractedSenderName = "Vikram Aditya" // Completely unmatched name
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(obligation))
        assertEquals(MatchDecision.NO_MATCH, evaluation.decision)
        assertNull(evaluation.bestMatch)
    }

    @Test
    fun test16_fullySettledObligationsExcluded() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        // Obligation is already fully settled in DB
        val settledObligation = Obligation(
            id = 10,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.ZERO,
            status = ObligationStatus.FULLY_SETTLED
        )
        val evidence = PaymentEvidence(
            imagePath = "/ev/1.png",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(settledObligation))
        assertEquals(MatchDecision.NO_MATCH, evaluation.decision)
        assertTrue(evaluation.allCandidates.isEmpty())
    }

    // ==========================================
    // SAFETY TESTS (17 - 20)
    // ==========================================

    @Test
    fun test17_duplicatePaymentEvidenceProtection() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val obligation = Obligation(
            id = 10,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L)
        )

        val existingEvidence = PaymentEvidence(
            id = 99,
            imagePath = "/evidence/prev.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh",
            utrNumber = "420194829104"
        )

        val incomingDuplicateEvidence = PaymentEvidence(
            id = 0,
            imagePath = "/evidence/new.jpg",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh",
            utrNumber = "420194829104" // Identical UTR!
        )

        val evaluation = engine.evaluate(
            evidence = incomingDuplicateEvidence,
            customers = listOf(customer),
            openObligations = listOf(obligation),
            existingEvidence = listOf(existingEvidence)
        )

        assertTrue(evaluation.isDuplicate)
        assertEquals(MatchDecision.NO_MATCH, evaluation.decision)
        assertNotNull(evaluation.duplicateWarning)
        assertTrue(evaluation.duplicateWarning!!.contains("420194829104"))
    }

    @Test
    fun test18_similarNamesWithDifferentObligations() {
        val customer1 = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val customer2 = Customer(id = 2, name = "Suresh", normalizedName = "suresh")

        val ob1 = Obligation(id = 101, customerId = 1, originalAmount = Money.fromRupees(500L), remainingAmount = Money.fromRupees(500L))
        val ob2 = Obligation(id = 102, customerId = 2, originalAmount = Money.fromRupees(500L), remainingAmount = Money.fromRupees(500L))

        val evidence = PaymentEvidence(
            imagePath = "/ev.png",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer1, customer2), listOf(ob1, ob2))
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
        assertEquals(1L, evaluation.bestMatch!!.customer.id)
    }

    @Test
    fun test19_sameCustomerMultipleOpenObligations_prefersExactAmount() {
        // Ramesh has two open debts: ₹500 and ₹300
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val ob500 = Obligation(
            id = 101,
            customerId = 1,
            originalAmount = Money.fromRupees(500L),
            remainingAmount = Money.fromRupees(500L),
            createdAt = 1000L
        )
        val ob300 = Obligation(
            id = 102,
            customerId = 1,
            originalAmount = Money.fromRupees(300L),
            remainingAmount = Money.fromRupees(300L),
            createdAt = 2000L
        )

        // Payment of ₹300 arrives from Ramesh
        val evidence = PaymentEvidence(
            imagePath = "/ev/300.jpg",
            extractedAmount = Money.fromRupees(300L),
            extractedSenderName = "Ramesh"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), listOf(ob500, ob300))
        assertEquals(MatchDecision.AUTO_MATCH, evaluation.decision)
        assertNotNull(evaluation.bestMatch)

        // Must prefer ob300 because it is an exact amount match rather than partial
        assertEquals(102L, evaluation.bestMatch!!.obligation.id)
        assertEquals(Money.fromRupees(300L), evaluation.bestMatch!!.obligation.remainingAmount)
        assertEquals(1.0f, evaluation.bestMatch!!.amountScore, 0.01f)
        assertEquals(SettlementOutcome.FULLY_SETTLED, evaluation.bestMatch!!.settlementPlan.outcome)

        // Verify the second candidate is ob500 with partial settlement
        assertEquals(2, evaluation.allCandidates.size)
        assertEquals(101L, evaluation.allCandidates[1].obligation.id)
        assertEquals(0.80f, evaluation.allCandidates[1].amountScore, 0.01f)
        assertEquals(SettlementOutcome.PARTIALLY_SETTLED, evaluation.allCandidates[1].settlementPlan.outcome)
    }

    @Test
    fun test20_noOpenObligations() {
        val customer = Customer(id = 1, name = "Ramesh", normalizedName = "ramesh")
        val evidence = PaymentEvidence(
            imagePath = "/ev.png",
            extractedAmount = Money.fromRupees(500L),
            extractedSenderName = "Ramesh"
        )

        val evaluation = engine.evaluate(evidence, listOf(customer), emptyList())
        assertEquals(MatchDecision.NO_MATCH, evaluation.decision)
        assertNull(evaluation.bestMatch)
        assertTrue(evaluation.allCandidates.isEmpty())
        assertTrue(evaluation.summaryExplanation.contains("No open credit obligations"))
    }
}
