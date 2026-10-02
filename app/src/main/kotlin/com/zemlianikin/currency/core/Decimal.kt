package com.zemlianikin.currency.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Десятичное число произвольной точности. Единственное место, где логика касается `java.math`:
 * остальной код знает только этот класс. Равенство, как у `BigDecimal`, учитывает scale (`1` ≠ `1.0`);
 * сравнивать значения — через [compareTo].
 */
class Decimal private constructor(val java: BigDecimal) : Comparable<Decimal> {
    /** @throws NumberFormatException */
    constructor(text: String) : this(BigDecimal(text))

    operator fun plus(other: Decimal) = Decimal(java.add(other.java))
    operator fun minus(other: Decimal) = Decimal(java.subtract(other.java))
    operator fun times(other: Decimal) = Decimal(java.multiply(other.java))

    /** Деление с точностью 34 значащих цифры. Делитель 0 — `ArithmeticException`. */
    operator fun div(other: Decimal) = Decimal(java.divide(other.java, MathContext.DECIMAL128))

    operator fun unaryMinus() = Decimal(java.negate())

    fun abs() = Decimal(java.abs())

    /** -1, 0 или 1. */
    fun signum(): Int = java.signum()

    fun movePointLeft(digits: Int) = Decimal(java.movePointLeft(digits))

    /** До [scale] знаков после запятой, половина — вверх. */
    fun rounded(scale: Int) = Decimal(java.setScale(scale, RoundingMode.HALF_UP))

    fun stripTrailingZeros() = Decimal(java.stripTrailingZeros())

    /** Без научной записи: `1E+3` → `1000`. */
    fun toPlainString(): String = java.toPlainString()

    override fun compareTo(other: Decimal): Int = java.compareTo(other.java)
    override fun equals(other: Any?): Boolean = other is Decimal && java == other.java
    override fun hashCode(): Int = java.hashCode()

    /** Точная запись; для очень малых и больших чисел — научная. Читается обратно конструктором. */
    override fun toString(): String = java.toString()

    companion object {
        val ONE = Decimal(BigDecimal.ONE)

        fun of(value: Long) = Decimal(BigDecimal.valueOf(value))

        /** null, если [text] — не число. */
        fun parseOrNull(text: String): Decimal? = text.toBigDecimalOrNull()?.let(::Decimal)
    }
}
