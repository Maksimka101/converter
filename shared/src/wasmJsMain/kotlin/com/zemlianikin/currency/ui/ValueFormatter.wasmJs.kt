package com.zemlianikin.currency.ui

import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.Decimal
import com.zemlianikin.currency.core.Num

private fun localeDecimalSeparator(): String =
    js("new Intl.NumberFormat().formatToParts(1.1).find(part => part.type === 'decimal').value")

/** Своими руками поверх [Decimal]: `Intl.NumberFormat` длинные числа точно не форматирует. Округление — как у jvm, к чётному. */
actual class ValueFormatter(actual val decimalSeparator: String) {
    actual constructor() : this(localeDecimalSeparator())

    actual fun amount(amount: Num): String = format(amount.value, min = 2, max = 2)

    actual fun value(value: Value): String = when (value) {
        is Value.Money -> "${amount(value.amount)} ${value.currency.code}"
        is Value.Number -> format(value.value.value, max = 8)
        is Value.Ratio -> {
            val change = (value.value.value - Decimal.ONE) * Decimal.of(100)
            "×${format(value.value.value, max = 4)} (${if (change.signum() >= 0) "+" else ""}${format(change, max = 1)}%)"
        }
    }

    /** Разряды неразрывным пробелом, после разделителя от [min] до [max] знаков. */
    private fun format(value: Decimal, max: Int, min: Int = 0): String {
        val plain = value.abs().roundedHalfEven(max).toPlainString()
        val whole = plain.substringBefore('.')
        val fraction = plain.substringAfter('.', "").trimEnd('0').padEnd(min, '0')
        return buildString {
            if (value.signum() < 0) append('-')
            whole.forEachIndexed { i, digit ->
                if (i > 0 && (whole.length - i) % 3 == 0) append(' ')
                append(digit)
            }
            if (fraction.isNotEmpty()) append(decimalSeparator).append(fraction)
        }
    }
}
