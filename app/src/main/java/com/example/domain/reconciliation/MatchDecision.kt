package com.example.domain.reconciliation

/**
 * Categorical decision made by the reconciliation engine for a match candidate.
 */
enum class MatchDecision {
    /**
     * High confidence: The candidate name and financial amount match strongly.
     * Can be linked automatically or pre-selected for one-tap reconciliation.
     */
    AUTO_MATCH,

    /**
     * Medium confidence: A plausible candidate exists (e.g. fuzzy name or compatible partial payment),
     * but human confirmation from the merchant is recommended.
     */
    SUGGESTED_MATCH,

    /**
     * Low confidence or unknown: No reliable candidate exists or identity could not be verified.
     */
    NO_MATCH
}
