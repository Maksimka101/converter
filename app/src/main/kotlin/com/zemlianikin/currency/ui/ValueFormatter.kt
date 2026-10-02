package com.zemlianikin.currency.ui

import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.Decimal
import com.zemlianikin.currency.core.Num
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Числа результата строкой. Форматирование временное: таблицы знаков валюты и локали ещё нет.
 * Разряды пробелом, как в вводе, но неразрывным: число не рвётся при переносе карточки. Лексер принимает
 * его как группировку, поэтому результат можно скопировать и вставить обратно.
 * `DecimalFormat` не потокобезопасен: экземпляр держит экран и зовёт только из UI.
 */
class ValueFormatter(locale: Locale = Locale.getDefault()) {
    private val symbols = DecimalFormatSymbols(locale).apply { groupingSeparator = '\u00A0' }
    private val money = format(min = 2, max = 2)
    private val number = format(max = 8)
    private val ratio = format(max = 4)
    private val percent = format(max = 1)

    /** Разделитель дроби локали: его же набирает numpad. */
    val decimalSeparator: String = symbols.decimalSeparator.toString()

    /** Сумма без кода валюты: всегда два знака после разделителя. */
    fun amount(amount: Num): String = money.format(amount.value.java)

    fun value(value: Value): String = when (value) {
        is Value.Money -> "${amount(value.amount)} ${value.currency.code}"
        is Value.Number -> number.format(value.value.value.java)
        is Value.Ratio -> {
            val change = (value.value.value - Decimal.ONE) * Decimal.of(100)
            "×${ratio.format(value.value.value.java)} (${if (change.signum() >= 0) "+" else ""}${percent.format(change.java)}%)"
        }
    }

    private fun format(max: Int, min: Int = 0) = DecimalFormat("#,##0." + "0".repeat(min) + "#".repeat(max - min), symbols)
}
