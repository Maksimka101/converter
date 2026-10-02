package com.zemlianikin.currency.ui

import com.zemlianikin.currency.core.CurrencyCode

/** Найденная валюта: [name] — название на языке интерфейса, [country] — страна, по которой нашли (если по ней). */
class CurrencyMatch(val code: CurrencyCode, val name: String, val country: String?)

/**
 * Поиск валюты по коду, слову словаря (`доллар`, `dollar`), названию валюты и названию страны (`швеция` → SEK).
 * Названия — на языке интерфейса, русском и английском.
 * Ищутся только валюты, которые понимает разбор (`Lexicon`): иначе вставленный код был бы ошибкой.
 * Порядок: точный код, начало кода, слово словаря, название валюты, страна; внутри — по [search] `ranking`.
 */
expect class CurrencyDirectory() {
    /** Пустой запрос — валюты рейтинга по порядку, чтобы выбрать из недавних. */
    fun search(query: String, ranking: List<CurrencyCode>, limit: Int = 20): List<CurrencyMatch>
}
