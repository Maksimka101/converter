package com.zemlianikin.currency.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** [DigitDecimal] сверяется с `BigDecimal` на случайных числах: `toString` выдаёт и цифры, и scale. */
class DigitDecimalTest {
    private val random = Random(20261008)

    private fun digits(max: Int) = buildString { repeat(random.nextInt(max + 1)) { append(random.nextInt(10)) } }

    private fun number(): String {
        val whole = when (random.nextInt(6)) {
            0 -> "0"
            1 -> digits(3) + "0".repeat(random.nextInt(12))
            2 -> digits(40)
            else -> digits(9)
        }.ifEmpty { "0" }
        val fraction = when (random.nextInt(5)) {
            0 -> ""
            1 -> "." + "0".repeat(random.nextInt(12)) + digits(4)
            2 -> "." + digits(40)
            else -> "." + digits(6)
        }
        val exponent = if (random.nextInt(6) == 0) "E${random.nextInt(-30, 31)}" else ""
        return (if (random.nextBoolean()) "-" else "") + whole + fraction + exponent
    }

    private fun same(expected: BigDecimal, actual: DigitDecimal, what: String) {
        assertEquals(expected.toString(), actual.toString(), what)
        assertEquals(expected.toPlainString(), actual.toPlainString(), "$what, plain")
        assertEquals(expected.signum(), actual.signum(), "$what, signum")
    }

    @Test
    fun `arithmetic matches BigDecimal`() {
        repeat(20_000) {
            val a = number()
            val b = number()
            val (ja, jb) = BigDecimal(a) to BigDecimal(b)
            val (da, db) = DigitDecimal(a) to DigitDecimal(b)
            same(ja, da, a)
            same(ja + jb, da + db, "$a + $b")
            same(ja - jb, da - db, "$a - $b")
            same(ja * jb, da * db, "$a * $b")
            if (jb.signum() != 0) same(ja.divide(jb, MathContext.DECIMAL128), da / db, "$a / $b")
            same(-ja, -da, "-$a")
            same(ja.abs(), da.abs(), "abs $a")
            same(ja.stripTrailingZeros(), da.stripTrailingZeros(), "strip $a")
            assertEquals(ja.compareTo(jb), da.compareTo(db), "$a <=> $b")
            assertEquals(ja == jb, da == db, "$a == $b")
            val places = random.nextInt(-5, 13)
            same(ja.movePointLeft(places), da.movePointLeft(places), "$a movePointLeft $places")
            same(ja.setScale(places, RoundingMode.HALF_UP), da.rounded(places), "$a rounded $places")
            same(ja.setScale(places, RoundingMode.HALF_EVEN), da.roundedHalfEven(places), "$a roundedHalfEven $places")
        }
    }

    @Test
    fun `division of simple numbers matches BigDecimal`() {
        val values = listOf("1", "2", "3", "4", "7", "8", "10", "1.0", "1.00", "0.5", "100", "1E+3", "0.001", "9999", "0", "0.00", "6", "2.50")
        for (a in values) for (b in values) {
            if (BigDecimal(b).signum() == 0) continue
            same(BigDecimal(a).divide(BigDecimal(b), MathContext.DECIMAL128), DigitDecimal(a) / DigitDecimal(b), "$a / $b")
            same(BigDecimal("-$a").divide(BigDecimal(b), MathContext.DECIMAL128), DigitDecimal("-$a") / DigitDecimal(b), "-$a / $b")
        }
    }

    @Test
    fun `halves round like BigDecimal`() {
        for (text in listOf("0.5", "1.5", "2.5", "-0.5", "-2.5", "0.125", "0.135", "2.5000", "2.5001", "0.004", "-0.004", "99.995", "0")) {
            for (scale in 0..3) {
                same(BigDecimal(text).setScale(scale, RoundingMode.HALF_UP), DigitDecimal(text).rounded(scale), "$text rounded $scale")
                same(BigDecimal(text).setScale(scale, RoundingMode.HALF_EVEN), DigitDecimal(text).roundedHalfEven(scale), "$text roundedHalfEven $scale")
            }
        }
    }

    @Test
    fun `parsing accepts and rejects the same text as BigDecimal`() {
        val texts = listOf(
            "", ".", "1.", ".5", "+1", "-", "+", "1e", "1e+", "1e5", "1E-5", "1e+5", "1.2.3", " 1", "1 ", "abc", "0x10", "--1", "+-1",
            "1e1.5", "00012.3400", "-0", "-0.00", "0e5", "0E-8", "e5", ".e5", "1e99999999999", "-.5e-3", "1_000", "1,5", "NaN", "Infinity",
        )
        for (text in texts) {
            val expected = text.toBigDecimalOrNull()
            val actual = DigitDecimal.parseOrNull(text)
            assertEquals(expected?.toString(), actual?.toString(), "\"$text\"")
        }
        assertEquals(BigDecimal.valueOf(Long.MIN_VALUE).toString(), DigitDecimal.of(Long.MIN_VALUE).toString())
        assertEquals("1", DigitDecimal.ONE.toString())
    }
}
