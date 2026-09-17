package com.example.domain.model

/**
 * Status of an open or closed credit obligation.
 */
enum class ObligationStatus {
    /**
     * Credit is fully open and awaiting payment.
     */
    OPEN,

    /**
     * Customer has made a partial payment, but balance remains.
     */
    PARTIALLY_SETTLED,

    /**
     * Credit has been completely paid off and reconciled.
     */
    FULLY_SETTLED,

    /**
     * Customer paid more than the credit amount (advance balance credited).
     */
    OVERPAID
}

/**
 * The outcome determined by the intelligent matching engine.
 */
enum class SettlementOutcome {
    /**
     * Payment evidence exactly or completely clears the obligation.
     */
    FULLY_SETTLED,

    /**
     * Payment evidence partially pays off the obligation.
     */
    PARTIALLY_SETTLED,

    /**
     * Payment evidence exceeds the obligation's remaining balance.
     */
    OVERPAID,

    /**
     * No matching customer or obligation could be determined with sufficient confidence.
     */
    NO_MATCH
}
