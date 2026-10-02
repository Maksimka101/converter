package com.zemlianikin.currency.core

/**
 * Десятичное число произвольной точности. Логика знает только этот класс; на JVM он поверх `java.math`.
 * Равенство учитывает scale (`1` ≠ `1.0`); сравнивать значения — через [compareTo].
 */
expect class Decimal : Comparable<Decimal> {
    /** @throws NumberFormatException */
    constructor(text: String)

    operator fun plus(other: Decimal): Decimal
    operator fun minus(other: Decimal): Decimal
    operator fun times(other: Decimal): Decimal

    /** Деление с точностью 34 значащих цифры. Делитель 0 — `ArithmeticException`. */
    operator fun div(other: Decimal): Decimal

    operator fun unaryMinus(): Decimal

    fun abs(): Decimal

    /** -1, 0 или 1. */
    fun signum(): Int

    fun movePointLeft(digits: Int): Decimal

    /** До [scale] знаков после запятой, половина — вверх. */
    fun rounded(scale: Int): Decimal

    fun stripTrailingZeros(): Decimal

    /** Без научной записи: `1E+3` → `1000`. */
    fun toPlainString(): String

    override fun compareTo(other: Decimal): Int

    /** Точная запись; для очень малых и больших чисел — научная. Читается обратно конструктором. */
    override fun toString(): String

    companion object {
        val ONE: Decimal

        fun of(value: Long): Decimal

        /** null, если [text] — не число. */
        fun parseOrNull(text: String): Decimal?
    }
}
