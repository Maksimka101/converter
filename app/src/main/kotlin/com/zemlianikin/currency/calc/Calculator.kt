package com.zemlianikin.currency.calc

import com.zemlianikin.currency.core.RateTable

// ГЕЙТ: менять только с разрешения пользователя.
/**
 * Строка ввода → результат. Чистая функция: без состояния и ввода-вывода.
 * UI вызывает заново при каждом изменении текста или курсов.
 */
interface Calculator {
    fun calculate(text: String, rates: RateTable): Calculation
}
