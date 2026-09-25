package com.zemlianikin.currency.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.zemlianikin.currency.calc.Calculation
import com.zemlianikin.currency.calc.Calculator
import com.zemlianikin.currency.calc.engine.Lexicon
import com.zemlianikin.currency.core.RateTable

/**
 * Правки текста за пользователя (#13). Каждая реагирует только на один набранный символ (или один
 * backspace): вставка, выделение и правки посреди слова текст не меняют. Не подошло — вернут `new`.
 */
fun editInput(old: TextFieldValue, new: TextFieldValue, calculator: Calculator, rates: RateTable): TextFieldValue {
    val opened = openParenOnClose(old, new, calculator, rates)
    if (opened !== new) return opened
    val spaced = autoSpace(old, new)
    if (spaced !== new) return spaced
    return eraseSpacedOperator(old, new)
}

/**
 * Набрана `)` без пары — дописывает в текст настоящую `(` там, где её поставил бы балансировщик (#5).
 * Скобка обычная: её можно стереть, дальше это уже явные скобки пользователя.
 */
fun openParenOnClose(
    old: TextFieldValue,
    new: TextFieldValue,
    calculator: Calculator,
    rates: RateTable,
): TextFieldValue {
    if (typedChar(old, new) != ')') return new
    val parens = (calculator.calculate(new.text, rates) as? Calculation.Ok)?.virtualParens.orEmpty()
    if (parens.isEmpty()) return new
    val text = StringBuilder(new.text)
    for (p in parens.sortedDescending()) text.insert(p, '(')
    return new.copy(text = text.toString(), selection = TextRange(new.selection.start + parens.size), composition = null)
}

/**
 * Пробелы при наборе (#13):
 * - вокруг бинарного оператора: `10+5` → `10 + 5`. Унарный (`-5`, `(-5)`, `* -5`) остаётся вплотную.
 *   Оператор бинарный, если слева операнд: цифра, `)`, `%`, слово или знак валюты;
 * - между числом (`)`, `%`) и словом или знаком валюты: `10usd` → `10 usd`;
 * - после целого слова словаря (валюта, `to`, `of`, множитель), если оно ничем не дорастает:
 *   `usd` → `usd `, но не `in` (это ещё `inr`) и не `руб` (`рубль`);
 * - пробел, набранный сразу после пробела (в том числе поставленного автоматически), игнорируется;
 * - `->` («to», #5): `-` с пробелами при вводе `>` становится `->`.
 */
fun autoSpace(old: TextFieldValue, new: TextFieldValue, lexicon: Lexicon = Lexicon.Default): TextFieldValue {
    val c = typedChar(old, new) ?: return new
    val cursor = new.selection.start
    val before = new.text.substring(0, cursor - 1)
    val after = new.text.substring(cursor)
    val prev = before.trimEnd().lastOrNull()
    return when {
        c == '>' && before.endsWith(" - ") -> new.withTail(before.dropLast(1) + ">", after)
        c in OPERATORS && prev != null && prev.endsOperand() ->
            new.withTail(before + (if (before.last().isWhitespace()) "" else " ") + c, after)
        c == ' ' && before.endsWith(" ") -> new.copy(text = old.text, selection = old.selection, composition = null)
        c.isLetter() || c.isCurrencySymbol() -> {
            val glued = before.lastOrNull()?.let { it.isDigit() || it == ')' || it == '%' } == true
            val spaced = if (glued) new.copy(
                text = before + " " + c + after,
                selection = TextRange(cursor + 1),
                composition = new.composition?.let { TextRange(it.start + if (it.start >= before.length) 1 else 0, it.end + 1) },
            ) else new
            spaced.spaceAfterWord(lexicon)
        }
        else -> new
    }
}

/** Курсор стоит сразу за целым словом, которое дальше не растёт: ставим за ним пробел. */
private fun TextFieldValue.spaceAfterWord(lexicon: Lexicon): TextFieldValue {
    val cursor = selection.start
    val after = text.substring(cursor)
    if (after.firstOrNull()?.let { Lexicon.isWordChar(it) || it == ' ' || it == ')' } == true) return this
    var start = cursor
    while (start > 0 && Lexicon.isWordChar(text[start - 1])) start--
    val word = text.substring(start, cursor)
    // Одиночный `$` — знак перед числом (`$10`), а не слово.
    if (word.none { it.isLetter() }) return this
    val meaning = lexicon.meaning(word)
    if (meaning == null || meaning is Lexicon.Meaning.Ambiguous || lexicon.canExtend(word)) return this
    return copy(text = text.substring(0, cursor) + " " + after, selection = TextRange(cursor + 1), composition = null)
}

/** [head] + пробел после него (если дальше нет пробела или `)`) + [after]; курсор — за пробелом. */
private fun TextFieldValue.withTail(head: String, after: String): TextFieldValue {
    val space = if (after.startsWith(" ") || after.startsWith(")")) "" else " "
    val skip = if (after.startsWith(" ")) 1 else 0
    return copy(text = head + space + after, selection = TextRange(head.length + space.length + skip), composition = null)
}

/**
 * Backspace по пробелу после ` + ` стирает оператор вместе с пробелами: одно нажатие вместо трёх,
 * а следующий оператор снова встанет с пробелами.
 */
fun eraseSpacedOperator(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (!old.selection.collapsed || !new.selection.collapsed) return new
    val at = new.selection.start
    val erased = old.text.length == new.text.length + 1 && old.selection.start == at + 1 &&
        new.text == old.text.removeRange(at, at + 1)
    if (!erased || at < 2 || old.text[at] != ' ' || old.text[at - 1] !in OPERATORS || old.text[at - 2] != ' ') return new
    // Справа операнд: оставляем один пробел, чтобы `10 + 5` не склеилось в `105`.
    val glued = old.text.getOrNull(at + 1)?.let { !it.isWhitespace() && it != ')' } == true
    val text = if (glued) old.text.removeRange(at - 1, at + 1) else old.text.removeRange(at - 2, at + 1)
    return new.copy(text = text, selection = TextRange(at - 2), composition = null)
}

/** Символ, который пользователь только что набрал в позиции курсора (единственная правка текста). */
private fun typedChar(old: TextFieldValue, new: TextFieldValue): Char? {
    val cursor = new.selection.start
    if (!old.selection.collapsed || !new.selection.collapsed || cursor <= 0) return null
    if (new.text.length != old.text.length + 1) return null
    return new.text[cursor - 1].takeIf { new.text.removeRange(cursor - 1, cursor) == old.text }
}

private const val OPERATORS = "+-*/×÷−"

/** Слово (валюта, `of`, `to`) считаем операндом: `of -5` получит лишние пробелы, парсер их не замечает. */
private fun Char.endsOperand() = isLetterOrDigit() || this == ')' || this == '%' || isCurrencySymbol()

private fun Char.isCurrencySymbol() = Character.getType(this) == Character.CURRENCY_SYMBOL.toInt()
