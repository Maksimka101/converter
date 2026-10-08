package com.zemlianikin.currency.ui

import com.zemlianikin.currency.calc.engine.Lexicon
import com.zemlianikin.currency.core.CurrencyCode

private fun browserLanguage(): String = js("navigator.language")

/** Название валюты на языке [locale]; null, если браузер кода не знает (тогда он возвращает сам код или бросает). */
private fun currencyName(locale: String, code: String): String? =
    js("(() => { try { const name = new Intl.DisplayNames([locale], { type: 'currency' }).of(code); return name && name !== code ? name : null } catch (e) { return null } })()")

/**
 * Названия берутся у браузера (`Intl.DisplayNames`). Поиска по стране нет: какой стране какая валюта, браузер
 * не сообщает. Порядок результатов — как в jvm-реализации.
 */
actual class CurrencyDirectory(
    private val lexicon: Lexicon,
    private val locales: List<String> = listOf(browserLanguage(), "ru", "en").distinct(),
) {
    actual constructor() : this(Lexicon.Default)

    private class Entry(val code: CurrencyCode, val name: String, val nameKeys: List<String>)

    // Строится при первом поиске: сотни вызовов в JS.
    private val entries: List<Entry> by lazy {
        lexicon.currenciesWithPrefix("").filter { lexicon.meaning(it.code) is Lexicon.Meaning.Currency }.map { code ->
            val names = locales.mapNotNull { currencyName(it, code.code) }
            Entry(code, names.firstOrNull() ?: code.code, names.map { it.lowercase() })
        }
    }

    /** Строка [key] начинается с [query] или содержит слово, которое с него начинается. */
    private fun matches(key: String, query: String) = key.startsWith(query) || key.contains(" $query") || key.contains("($query")

    actual fun search(query: String, ranking: List<CurrencyCode>, limit: Int): List<CurrencyMatch> {
        val q = query.trim().lowercase()
        fun rank(code: CurrencyCode) = ranking.indexOf(code).let { if (it < 0) Int.MAX_VALUE else it }
        if (q.isEmpty()) {
            val byCode = entries.associateBy { it.code }
            return ranking.mapNotNull { byCode[it] }.take(limit).map { CurrencyMatch(it.code, it.name, null) }
        }
        val words = lexicon.currenciesWithPrefix(q).toSet()
        class Hit(val score: Int, val entry: Entry)
        return entries.mapNotNull { e ->
            val code = e.code.code.lowercase()
            val score = when {
                code == q -> 0
                code.startsWith(q) -> 1
                e.code in words -> 2
                e.nameKeys.any { matches(it, q) } -> 3
                else -> return@mapNotNull null
            }
            Hit(score, e)
        }.sortedWith(compareBy({ it.score }, { rank(it.entry.code) }, { it.entry.code.code }))
            .take(limit).map { CurrencyMatch(it.entry.code, it.entry.name, null) }
    }
}
