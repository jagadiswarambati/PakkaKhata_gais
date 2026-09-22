package com.example.domain.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Money representation in integer paise (1 Rupee = 100 Paise).
 * Financial balances and settlement calculations use integer paise to avoid rounding errors.
 */
@JvmInline
value class Money(val paise: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(this.paise + other.paise)
    operator fun minus(other: Money): Money = Money(this.paise - other.paise)
    operator fun times(factor: Long): Money = Money(this.paise * factor)
    operator fun unaryMinus(): Money = Money(-this.paise)

    override fun compareTo(other: Money): Int = this.paise.compareTo(other.paise)

    val isZero: Boolean get() = paise == 0L
    val isPositive: Boolean get() = paise > 0L
    val isNegative: Boolean get() = paise < 0L

    val rupeesWhole: Long get() = paise / 100
    val paiseRemainder: Long get() = kotlin.math.abs(paise % 100)

    /**
     * Formats the money into a clean Indian Rupee string, e.g. "₹500" or "₹500.50".
     */
    fun formatRupees(includeDecimalsIfZero: Boolean = false): String {
        val isNeg = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val remainder = absPaise % 100

        val numberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        val formattedRupees = numberFormat.format(rupees)

        val prefix = if (isNeg) "-₹" else "₹"
        return if (remainder == 0L && !includeDecimalsIfZero) {
            "$prefix$formattedRupees"
        } else {
            "$prefix$formattedRupees.%02d".format(remainder)
        }
    }

    override fun toString(): String = formatRupees()

    companion object {
        val ZERO = Money(0L)

        fun fromRupees(rupees: Long): Money = Money(rupees * 100L)

        fun fromRupees(rupees: Double): Money {
            val paise = Math.round(rupees * 100.0)
            return Money(paise)
        }

        fun fromPaise(paise: Long): Money = Money(paise)
    }
}
