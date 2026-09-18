package com.example.domain.reconciliation

import com.example.domain.model.Customer
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import kotlin.math.max

/**
 * Pure Kotlin deterministic implementation of the ReconciliationEngine.
 * Implements candidate generation, name normalization, fuzzy similarity scoring,
 * explainable weighted evaluation, decision thresholding, and settlement calculations.
 */
class DefaultReconciliationEngine(
    private val config: ReconciliationConfig = ReconciliationConfig.DEFAULT
) : ReconciliationEngine {

    override fun evaluate(
        evidence: PaymentEvidence,
        customers: List<Customer>,
        openObligations: List<Obligation>,
        existingEvidence: List<PaymentEvidence>
    ): ReconciliationEvaluation {
        // 1. Check for Duplicate Payment Evidence
        val duplicateCheck = DuplicateDetector.checkForDuplicate(evidence, existingEvidence)
        if (duplicateCheck.isDuplicate) {
            return ReconciliationEvaluation(
                evidence = evidence,
                bestMatch = null,
                decision = MatchDecision.NO_MATCH,
                allCandidates = emptyList(),
                isDuplicate = true,
                duplicateWarning = duplicateCheck.reason,
                summaryExplanation = "Duplicate payment evidence detected: ${duplicateCheck.reason}"
            )
        }

        // 2. Find and rank candidates
        val candidates = findCandidates(evidence, customers, openObligations)

        if (candidates.isEmpty()) {
            return ReconciliationEvaluation(
                evidence = evidence,
                bestMatch = null,
                decision = MatchDecision.NO_MATCH,
                allCandidates = emptyList(),
                summaryExplanation = if (openObligations.isEmpty()) {
                    "No open credit obligations found in ledger"
                } else {
                    "No matching customer found for sender '${evidence.extractedSenderName ?: "Unknown"}'"
                }
            )
        }

        val bestCandidate = candidates.first()
        val decision = bestCandidate.matchDecision

        val summaryExplanation = when (decision) {
            MatchDecision.AUTO_MATCH -> {
                "High confidence match: '${bestCandidate.customer.name}' for obligation #${bestCandidate.obligation.id} (${(bestCandidate.overallScore * 100).toInt()}% confidence). ${bestCandidate.settlementPlan.outcome}"
            }
            MatchDecision.SUGGESTED_MATCH -> {
                "Suggested match: '${bestCandidate.customer.name}' for obligation #${bestCandidate.obligation.id} (${(bestCandidate.overallScore * 100).toInt()}% confidence). Confirmation recommended."
            }
            MatchDecision.NO_MATCH -> {
                "No candidate met confidence threshold for automatic matching (best score ${(bestCandidate.overallScore * 100).toInt()}%)."
            }
        }

        return ReconciliationEvaluation(
            evidence = evidence,
            bestMatch = if (decision != MatchDecision.NO_MATCH) bestCandidate else null,
            decision = decision,
            allCandidates = candidates,
            summaryExplanation = summaryExplanation
        )
    }

    override fun findCandidates(
        evidence: PaymentEvidence,
        customers: List<Customer>,
        openObligations: List<Obligation>
    ): List<MatchCandidate> {
        val customerMap = customers.associateBy { it.id }

        // Filter: Open obligations only. Fully settled/overpaid obligations are excluded.
        val validObligations = openObligations.filter { obligation ->
            (obligation.status == ObligationStatus.OPEN || obligation.status == ObligationStatus.PARTIALLY_SETTLED) &&
                    obligation.remainingAmount.paise > 0L
        }

        if (validObligations.isEmpty()) {
            return emptyList()
        }

        val candidateList = mutableListOf<MatchCandidate>()

        for (obligation in validObligations) {
            val customer = customerMap[obligation.customerId] ?: continue
            val candidate = scoreCandidate(evidence, customer, obligation)
            candidateList.add(candidate)
        }

        // Rank candidates:
        // 1. Overall score descending
        // 2. Amount score descending (exact amount preferred over partial)
        // 3. FIFO: earlier obligations first
        return candidateList.sortedWith(
            compareByDescending<MatchCandidate> { it.overallScore }
                .thenByDescending { it.amountScore }
                .thenBy { it.obligation.createdAt }
        )
    }

    override suspend fun findMatches(
        evidence: PaymentEvidence,
        openObligations: List<Obligation>
    ): List<MatchCandidate> {
        // Synthesize customer placeholders from obligations if direct customer list not provided
        val syntheticCustomers = openObligations.map { ob ->
            Customer(
                id = ob.customerId,
                name = ob.notes ?: "Customer #${ob.customerId}",
                normalizedName = NameNormalizer.normalize(ob.notes ?: "Customer #${ob.customerId}")
            )
        }
        return findCandidates(evidence, syntheticCustomers, openObligations)
    }

    /**
     * Decomposes matching signals, applies explainable weighting, enforces identity guardrails,
     * and deterministically calculates the settlement plan.
     */
    private fun scoreCandidate(
        evidence: PaymentEvidence,
        customer: Customer,
        obligation: Obligation
    ): MatchCandidate {
        val matchReasons = mutableListOf<String>()

        // 1. Name Similarity Signal
        val senderRaw = evidence.extractedSenderName
        val nameScore = if (senderRaw.isNullOrBlank()) {
            matchReasons.add("Sender name missing from payment evidence")
            0.0f
        } else {
            val simName = FuzzyMatcher.calculateNameSimilarity(senderRaw, customer.name)
            val simNorm = FuzzyMatcher.calculateNameSimilarity(senderRaw, customer.normalizedName)
            val bestNameScore = max(simName, simNorm)

            when {
                bestNameScore >= 0.98f -> matchReasons.add("Exact name match with customer '${customer.name}'")
                bestNameScore >= 0.85f -> matchReasons.add("Strong name match (${(bestNameScore * 100).toInt()}%) with '${customer.name}'")
                bestNameScore >= 0.50f -> matchReasons.add("Fuzzy name similarity (${(bestNameScore * 100).toInt()}%) with '${customer.name}'")
                else -> matchReasons.add("Name does not match '${customer.name}'")
            }
            bestNameScore
        }

        // 2. Amount Relevance Signal
        val paidPaise = evidence.extractedAmount.paise
        val remainingPaise = obligation.remainingAmount.paise

        val amountScore = when {
            paidPaise <= 0L -> {
                matchReasons.add("Invalid/zero payment amount")
                0.0f
            }
            paidPaise == remainingPaise -> {
                matchReasons.add("Payment matches exact remaining balance (${obligation.remainingAmount.formatRupees()})")
                1.0f
            }
            paidPaise < remainingPaise -> {
                matchReasons.add("Payment is compatible partial settlement (${evidence.extractedAmount.formatRupees()} of ${obligation.remainingAmount.formatRupees()})")
                0.80f
            }
            else -> {
                // paidPaise > remainingPaise
                val excess = evidence.extractedAmount - obligation.remainingAmount
                matchReasons.add("Payment exceeds obligation balance by ${excess.formatRupees()}")
                0.65f
            }
        }

        // 3. Temporal Relevance Signal
        val temporalScore = if (evidence.timestamp >= obligation.createdAt) {
            matchReasons.add("Payment recorded after obligation creation")
            1.0f
        } else {
            matchReasons.add("Payment timestamp is prior to obligation creation")
            0.30f
        }

        // 4. Deterministic Settlement Plan
        val settlementPlan = SettlementCalculator.calculate(obligation, evidence)

        // 5. Identity Guardrail: Amount must NOT override identity.
        // If name similarity is below the suggested threshold or absent, overall score is zeroed out
        // to prevent allocating debt to a stranger solely based on amount matching.
        val overallScore = if (nameScore < config.minNameScoreForSuggested) {
            if (!senderRaw.isNullOrBlank()) {
                matchReasons.add("Identity guardrail: rejected candidate due to name mismatch")
            }
            0.0f
        } else {
            (nameScore * config.nameWeight) +
                    (amountScore * config.amountWeight) +
                    (temporalScore * config.temporalWeight)
        }

        // 6. Match Decision Thresholding
        val matchDecision = when {
            overallScore >= config.autoMatchThreshold && nameScore >= config.minNameScoreForAuto -> {
                MatchDecision.AUTO_MATCH
            }
            overallScore >= config.suggestedMatchThreshold && nameScore >= config.minNameScoreForSuggested -> {
                MatchDecision.SUGGESTED_MATCH
            }
            else -> {
                MatchDecision.NO_MATCH
            }
        }

        return MatchCandidate(
            customer = customer,
            obligation = obligation,
            overallScore = overallScore,
            nameScore = nameScore,
            amountScore = amountScore,
            temporalScore = temporalScore,
            matchDecision = matchDecision,
            settlementPlan = settlementPlan,
            matchReasons = matchReasons
        )
    }
}
