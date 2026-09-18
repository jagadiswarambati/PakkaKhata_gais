package com.example.domain.reconciliation

/**
 * Centralized, explainable configuration parameters for the reconciliation matching engine.
 * Eliminates magic numbers across the codebase and allows easy threshold tuning.
 */
data class ReconciliationConfig(
    // Decision confidence thresholds
    val autoMatchThreshold: Float = 0.80f,
    val suggestedMatchThreshold: Float = 0.50f,

    // Identity validation thresholds (Amount must NOT override identity)
    val minNameScoreForAuto: Float = 0.70f,
    val minNameScoreForSuggested: Float = 0.40f,

    // Scoring signal weights (Must sum to 1.0f)
    val nameWeight: Float = 0.65f,
    val amountWeight: Float = 0.30f,
    val temporalWeight: Float = 0.05f
) {
    companion object {
        val DEFAULT = ReconciliationConfig()
    }
}
