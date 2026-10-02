package com.zemlianikin.currency.ui

import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.Num

/**
 * Числа результата строкой в локали устройства. Форматирование временное: таблицы знаков валюты и локали ещё нет.
 * Разряды пробелом, как в вводе, но неразрывным: число не рвётся при переносе карточки. Лексер принимает
 * его как группировку, поэтому результат можно скопировать и вставить обратно.
 * Не потокобезопасен: экземпляр держит экран и зовёт только из UI.
 */
expect class ValueFormatter() {
    /** Разделитель дроби локали: его же набирает numpad. */
    val decimalSeparator: String

    /** Сумма без кода валюты: всегда два знака после разделителя. */
    fun amount(amount: Num): String

    fun value(value: Value): String
}
