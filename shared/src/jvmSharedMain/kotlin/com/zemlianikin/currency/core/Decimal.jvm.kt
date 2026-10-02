package com.zemlianikin.currency.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Единственное место, где логика касается `java.math`. Равенство — как у `BigDecimal`. */
actual class Decimal private constructor(val java: BigDecimal) : Comparable<Decimal> {
    actual constructor(text: String) : this(BigDecimal(text))

    actual operator fun plus(other: Decimal) = Decimal(java.add(other.java))
    actual operator fun minus(other: Decimal) = Decimal(java.subtract(other.java))
    actual operator fun times(other: Decimal) = Decimal(java.multiply(other.java))
    actual operator fun div(other: Decimal) = Decimal(java.divide(other.java, MathContext.DECIMAL128))
    actual operator fun unaryMinus() = Decimal(java.negate())

    actual fun abs() = Decimal(java.abs())
    actual fun signum(): Int = java.signum()
    actual fun movePointLeft(digits: Int) = Decimal(java.movePointLeft(digits))
    actual fun rounded(scale: Int) = Decimal(java.setScale(scale, RoundingMode.HALF_UP))
    actual fun stripTrailingZeros() = Decimal(java.stripTrailingZeros())
    actual fun toPlainString(): String = java.toPlainString()

    actual override fun compareTo(other: Decimal): Int = java.compareTo(other.java)
    override fun equals(other: Any?): Boolean = other is Decimal && java == other.java
    override fun hashCode(): Int = java.hashCode()
    actual override fun toString(): String = java.toString()

    actual companion object {
        actual val ONE = Decimal(BigDecimal.ONE)

        actual fun of(value: Long) = Decimal(BigDecimal.valueOf(value))

        actual fun parseOrNull(text: String): Decimal? = text.toBigDecimalOrNull()?.let(::Decimal)
    }
}
