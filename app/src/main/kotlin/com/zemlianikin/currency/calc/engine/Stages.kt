package com.zemlianikin.currency.calc.engine

import com.zemlianikin.currency.calc.Calculation
import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.RateTable

// Конвейер: text → Lexer → Balancer → Parser → Evaluator → Value (#3).
// Этап, который не может продолжить, бросает Stop; Calculator превращает его в результат.

// ГЕЙТ: менять только с разрешения пользователя.
/** Досрочный результат конвейера: Incomplete или Failed. Без стектрейса — это не баг. */
class Stop(val outcome: Calculation) : Exception(null, null, false, false)

// ГЕЙТ: менять только с разрешения пользователя.
/** Текст → плоский список слов. Ловит неизвестные, неоднозначные слова и плохие числа. */
interface Lexer {
    /** @throws Stop */
    fun lex(text: String): List<Token>
}

// ГЕЙТ: менять только с разрешения пользователя.
/** Токены со скобками парами + где рисовать виртуальные '('. */
data class Balanced(val tokens: List<Token>, val virtualParens: List<Int>)

// ГЕЙТ: менять только с разрешения пользователя.
/** Дополняет скобки до парных по правилам R1b и R2 (#5). Ошибок не бросает: лишнюю ')' ловит парсер. */
interface Balancer {
    fun balance(tokens: List<Token>): Balanced
}

// ГЕЙТ: менять только с разрешения пользователя.
/** Токены с парными скобками → дерево. Упал на конце ввода — Incomplete, раньше — Failed. */
interface Parser {
    /** @throws Stop */
    fun parse(tokens: List<Token>): Node
}

// ГЕЙТ: менять только с разрешения пользователя.
/** Дерево + курсы → значение по правилам типов #5. Все ошибки — Failed: дерево уже полное. */
interface Evaluator {
    /** @throws Stop */
    fun eval(node: Node, rates: RateTable): Value
}
