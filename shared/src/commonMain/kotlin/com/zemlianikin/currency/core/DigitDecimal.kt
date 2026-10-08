package com.zemlianikin.currency.core

/**
 * [Decimal] на чистом Kotlin для целей без `java.math`: знак, цифры и scale, значение = ±цифры × 10^-scale.
 * Результаты, их scale и запись — как у `java.math.BigDecimal`; сверяется с ним тестом на jvm.
 * Арифметика в столбик по десятичным цифрам: числа калькулятора — десятки цифр, скорость не важна.
 */
class DigitDecimal private constructor(
    private val negative: Boolean,
    /** Цифры от младшей к старшей, без старших нулей; у нуля пусто. */
    private val digits: IntArray,
    private val scale: Int,
) : Comparable<DigitDecimal> {

    private constructor(other: DigitDecimal) : this(other.negative, other.digits, other.scale)

    /** @throws NumberFormatException */
    constructor(text: String) : this(parse(text))

    operator fun plus(other: DigitDecimal): DigitDecimal {
        val common = maxOf(scale, other.scale)
        val a = shift(digits, common - scale)
        val b = shift(other.digits, common - other.scale)
        return when {
            negative == other.negative -> make(negative, add(a, b), common)
            compare(a, b) >= 0 -> make(negative, subtract(a, b), common)
            else -> make(other.negative, subtract(b, a), common)
        }
    }

    operator fun minus(other: DigitDecimal) = this + -other

    operator fun times(other: DigitDecimal) = make(negative != other.negative, multiply(digits, other.digits), scale + other.scale)

    /** Деление с точностью 34 значащих цифры, половина — к чётному. Делитель 0 — `ArithmeticException`. */
    operator fun div(other: DigitDecimal): DigitDecimal {
        if (other.digits.isEmpty()) throw ArithmeticException("Деление на ноль")
        val preferred = scale - other.scale
        if (digits.isEmpty()) return make(false, digits, preferred)
        // Сдвиг делимого, при котором у частного ровно PRECISION цифр: с первой попытки их PRECISION или на одну меньше.
        var raise = PRECISION - 1 - (digits.size - other.digits.size)
        var divisor = other.digits
        var quotient = digits
        var remainder = digits
        for (attempt in 0..1) {
            divisor = shift(other.digits, maxOf(-raise, 0))
            val (q, r) = divide(shift(digits, maxOf(raise, 0)), divisor)
            quotient = q
            remainder = r
            if (q.size == PRECISION) break
            raise++
        }
        val half = compare(add(remainder, remainder), divisor)
        if (half > 0 || half == 0 && quotient[0] % 2 == 1) {
            quotient = add(quotient, ONE_DIGIT)
            if (quotient.size > PRECISION) {
                quotient = quotient.copyOfRange(1, quotient.size)
                raise--
            }
        }
        val result = make(negative != other.negative, quotient, preferred + raise)
        return if (remainder.isEmpty()) result.stripTrailingZeros(preferred) else result
    }

    operator fun unaryMinus() = make(!negative, digits, scale)

    fun abs() = make(false, digits, scale)

    /** -1, 0 или 1. */
    fun signum(): Int = if (digits.isEmpty()) 0 else if (negative) -1 else 1

    fun movePointLeft(digits: Int): DigitDecimal {
        val moved = scale + digits
        return if (moved < 0) make(negative, shift(this.digits, -moved), 0) else make(negative, this.digits, moved)
    }

    /** До [scale] знаков после запятой, половина — вверх. */
    fun rounded(scale: Int) = rounded(scale, halfEven = false)

    /** До [scale] знаков после запятой, половина — к чётному. */
    fun roundedHalfEven(scale: Int) = rounded(scale, halfEven = true)

    private fun rounded(target: Int, halfEven: Boolean): DigitDecimal {
        val drop = scale - target
        if (drop <= 0) return make(negative, shift(digits, -drop), target)
        val kept = if (drop >= digits.size) NO_DIGITS else digits.copyOfRange(drop, digits.size)
        val first = digits.getOrElse(drop - 1) { 0 }
        val up = when {
            first != 5 || !halfEven -> first >= 5
            (0 until drop - 1).any { digits[it] != 0 } -> true
            else -> kept.isNotEmpty() && kept[0] % 2 == 1
        }
        return make(negative, if (up) add(kept, ONE_DIGIT) else kept, target)
    }

    fun stripTrailingZeros(): DigitDecimal = if (digits.isEmpty()) make(false, digits, 0) else stripTrailingZeros(Int.MIN_VALUE)

    /** Убирает нули справа, пока scale больше [minScale]. */
    private fun stripTrailingZeros(minScale: Int): DigitDecimal {
        var zeros = 0
        while (zeros < digits.size - 1 && digits[zeros] == 0 && scale - zeros > minScale) zeros++
        return if (zeros == 0) this else make(negative, digits.copyOfRange(zeros, digits.size), scale - zeros)
    }

    /** Без научной записи: `1E+3` → `1000`. */
    fun toPlainString(): String = when {
        digits.isEmpty() && scale <= 0 -> "0"
        scale <= 0 -> sign() + digitsText() + "0".repeat(-scale)
        else -> sign() + withPoint(digitsText(), scale)
    }

    override fun compareTo(other: DigitDecimal): Int {
        if (negative != other.negative) return if (negative) -1 else 1
        val common = maxOf(scale, other.scale)
        val magnitude = compare(shift(digits, common - scale), shift(other.digits, common - other.scale))
        return if (negative) -magnitude else magnitude
    }

    /** Равенство учитывает scale (`1` ≠ `1.0`). */
    override fun equals(other: Any?): Boolean =
        other is DigitDecimal && negative == other.negative && scale == other.scale && digits.contentEquals(other.digits)

    override fun hashCode(): Int = 31 * (31 * digits.contentHashCode() + scale) + negative.hashCode()

    /** Точная запись; для очень малых и больших чисел — научная. Читается обратно конструктором. */
    override fun toString(): String {
        val text = digitsText()
        val exponent = text.length - 1 - scale.toLong()
        if (scale >= 0 && exponent >= -6) return sign() + if (scale == 0) text else withPoint(text, scale)
        val mantissa = if (text.length == 1) text else text.take(1) + "." + text.drop(1)
        return sign() + mantissa + when {
            exponent == 0L -> ""
            exponent > 0 -> "E+$exponent"
            else -> "E$exponent"
        }
    }

    private fun sign() = if (negative) "-" else ""

    private fun digitsText(): String = if (digits.isEmpty()) "0" else buildString { for (i in digits.indices.reversed()) append(digits[i]) }

    companion object {
        /** Значащих цифр у частного: как `MathContext.DECIMAL128`. */
        private const val PRECISION = 34

        private val NO_DIGITS = IntArray(0)
        private val ONE_DIGIT = intArrayOf(1)

        val ONE = DigitDecimal("1")

        fun of(value: Long) = DigitDecimal(value.toString())

        /** null, если [text] — не число. */
        fun parseOrNull(text: String): DigitDecimal? = try {
            DigitDecimal(text)
        } catch (_: NumberFormatException) {
            null
        }

        private fun make(negative: Boolean, digits: IntArray, scale: Int) = DigitDecimal(negative && digits.isNotEmpty(), digits, scale)

        /** `[+-]цифры[.цифры][e[+-]цифры]`, точка может стоять с краю: `1.`, `.5`. */
        private fun parse(text: String): DigitDecimal {
            fun fail(): Nothing = throw NumberFormatException("Не число: $text")
            val signed = text.startsWith('-') || text.startsWith('+')
            val exponentAt = text.indexOfFirst { it == 'e' || it == 'E' }
            val mantissa = text.substring(if (signed) 1 else 0, if (exponentAt < 0) text.length else exponentAt)
            val point = mantissa.indexOf('.')
            val fraction = if (point < 0) 0 else mantissa.length - point - 1
            val all = if (point < 0) mantissa else mantissa.removeRange(point, point + 1)
            if (all.isEmpty() || all.any { it !in '0'..'9' }) fail()
            val exponent = if (exponentAt < 0) 0 else text.substring(exponentAt + 1).toIntOrNull() ?: fail()
            val scale = fraction.toLong() - exponent
            if (scale < Int.MIN_VALUE || scale > Int.MAX_VALUE) fail()
            val digits = trim(IntArray(all.length) { all[all.length - 1 - it] - '0' })
            return make(text.startsWith('-'), digits, scale.toInt())
        }

        /** Цифры с точкой за [scale] знаков от конца: `("5", 3)` → `0.005`. */
        private fun withPoint(text: String, scale: Int): String =
            if (text.length > scale) text.dropLast(scale) + "." + text.takeLast(scale)
            else "0." + "0".repeat(scale - text.length) + text

        private fun trim(a: IntArray): IntArray {
            var size = a.size
            while (size > 0 && a[size - 1] == 0) size--
            return if (size == a.size) a else a.copyOf(size)
        }

        private fun compare(a: IntArray, b: IntArray): Int {
            if (a.size != b.size) return a.size.compareTo(b.size)
            for (i in a.indices.reversed()) if (a[i] != b[i]) return a[i].compareTo(b[i])
            return 0
        }

        private fun add(a: IntArray, b: IntArray): IntArray {
            val out = IntArray(maxOf(a.size, b.size) + 1)
            var carry = 0
            for (i in out.indices) {
                val sum = a.getOrElse(i) { 0 } + b.getOrElse(i) { 0 } + carry
                out[i] = sum % 10
                carry = sum / 10
            }
            return trim(out)
        }

        /** a − b при a ≥ b. */
        private fun subtract(a: IntArray, b: IntArray): IntArray {
            val out = IntArray(a.size)
            var borrow = 0
            for (i in a.indices) {
                val difference = a[i] - b.getOrElse(i) { 0 } - borrow
                out[i] = (difference + 10) % 10
                borrow = if (difference < 0) 1 else 0
            }
            return trim(out)
        }

        private fun multiply(a: IntArray, b: IntArray): IntArray {
            if (a.isEmpty() || b.isEmpty()) return NO_DIGITS
            val out = IntArray(a.size + b.size)
            for (i in a.indices) {
                var carry = 0
                for (j in b.indices) {
                    val sum = out[i + j] + a[i] * b[j] + carry
                    out[i + j] = sum % 10
                    carry = sum / 10
                }
                out[i + b.size] = carry
            }
            return trim(out)
        }

        /** a × 10^[places], [places] ≥ 0. */
        private fun shift(a: IntArray, places: Int): IntArray =
            if (a.isEmpty() || places == 0) a else IntArray(a.size + places).also { a.copyInto(it, places) }

        /** Частное и остаток a / b при b ≠ 0: по цифре за шаг, цифра — вычитаниями. */
        private fun divide(a: IntArray, b: IntArray): Pair<IntArray, IntArray> {
            val quotient = IntArray(a.size)
            var remainder = NO_DIGITS
            for (i in a.indices.reversed()) {
                remainder = trim(IntArray(remainder.size + 1).also { remainder.copyInto(it, 1); it[0] = a[i] })
                while (compare(remainder, b) >= 0) {
                    remainder = subtract(remainder, b)
                    quotient[i]++
                }
            }
            return trim(quotient) to remainder
        }
    }
}
