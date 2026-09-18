package com.example.domain.reconciliation

import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome

/**
 * Result of a deterministic settlement calculation between an obligation and a payment evidence.
 * Safely calculated using integer paise with zero floating-point loss.
 */
data class SettlementPlan(
    val settledAmount: Money,
    val newRemainingAmount: Money,
    val excessAmount: Money,
    val outcome: SettlementOutcome
)

/**
 * Deterministic settlement calculator based on PakkaKhata rules:
 * - Paid == Target -> FULLY_SETTLED (remaining = 0, excess = 0)
 * - Paid < Target  -> PARTIALLY_SETTLED (remaining = Target - Paid, excess = 0)
 * - Paid > Target  -> OVERPAID (remaining = 0, excess = Paid - Target, settled = Target)
 * - Paid <= 0      -> NO_MATCH (remaining = Target, excess = 0, settled = 0)
 */
object SettlementCalculator {

    fun calculate(
        obligation: Obligation,
        evidence: PaymentEvidence
    ): SettlementPlan {
        return calculate(
            remainingAmount = obligation.remainingAmount,
            paidAmount = evidence.extractedAmount
        )
    }

    fun calculate(
        remainingAmount: Money,
        paidAmount: Money
    ): SettlementPlan {
        val targetPaise = remainingAmount.paise
        val paidPaise = paidAmount.paise

        // Zero or negative payment amounts are invalid for settlement
        if (paidPaise <= 0L) {
            return SettlementPlan(
                settledAmount = Money.ZERO,
                newRemainingAmount = remainingAmount,
                excessAmount = Money.ZERO,
                outcome = SettlementOutcome.NO_MATCH
            )
        }

        return when {
            paidPaise == targetPaise -> {
                SettlementPlan(
                    settledAmount = Money.fromPaise(targetPaise),
                    newRemainingAmount = Money.ZERO,
                    excessAmount = Money.ZERO,
                    outcome = SettlementOutcome.FULLY_SETTLED
                )
            }
            paidPaise < targetPaise -> {
                SettlementPlan(
                    settledAmount = Money.fromPaise(paidPaise),
                    newRemainingAmount = Money.fromPaise(targetPaise - paidPaise),
                    excessAmount = Money.ZERO,
                    outcome = SettlementOutcome.PARTIALLY_SETTLED
                )
            }
            else -> {
                // Overpayment: paidPaise > targetPaise
                SettlementPlan(
                    settledAmount = Money.fromPaise(targetPaise),
                    newRemainingAmount = Money.ZERO,
                    excessAmount = Money.fromPaise(paidPaise - targetPaise),
                    outcome = SettlementOutcome.OVERPAID
                )
            }
        }
    }
}
