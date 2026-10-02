package com.zemlianikin.currency.ui

import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.Decimal
import com.zemlianikin.currency.core.Num
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** На `DecimalFormat`; он и не потокобезопасен. */
actual class ValueFormatter(locale: Locale) {
    actual constructor() : this(Locale.getDefault())

    private val symbols = DecimalFormatSymbols(locale).apply { groupingSeparator = '\u00A0' }
    private val money = format(min = 2, max = 2)
    private val number = format(max = 8)
    private val ratio = format(max = 4)
    private val percent = format(max = 1)

    actual val decimalSeparator: String = symbols.decimalSeparator.toString()

    actual fun amount(amount: Num): String = money.format(amount.value.java)

    actual fun value(value: Value): String = when (value) {
        is Value.Money -> "${amount(value.amount)} ${value.currency.code}"
        is Value.Number -> number.format(value.value.value.java)
        is Value.Ratio -> {
            val change = (value.value.value - Decimal.ONE) * Decimal.of(100)
            "×${ratio.format(value.value.value.java)} (${if (change.signum() >= 0) "+" else ""}${percent.format(change.java)}%)"
        }
    }

    private fun format(max: Int, min: Int = 0) = DecimalFormat("#,##0." + "0".repeat(min) + "#".repeat(max - min), symbols)
}
