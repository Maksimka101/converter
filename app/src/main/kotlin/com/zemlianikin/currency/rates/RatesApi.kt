package com.zemlianikin.currency.rates

import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.Num
import com.zemlianikin.currency.core.RateTable
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate

// ГЕЙТ: менять только с разрешения пользователя.
/**
 * Курсы в виде, который можно сохранить и перечислить (`RateTable` состав не отдаёт).
 * [perBase] — сколько единиц валюты за 1 базовую (USD), у самой базы 1.
 */
class RatesSnapshot(val date: LocalDate, val perBase: Map<CurrencyCode, Num>) {
    val currencies: List<CurrencyCode> get() = perBase.keys.toList()

    fun toTable(): RateTable = RateTable(date, perBase)
}

// ГЕЙТ: менять только с разрешения пользователя.
/** Источник курсов. Один запрос — вся таблица. При сбое бросает исключение (сеть, формат). */
interface RatesProvider {
    suspend fun fetch(): RatesSnapshot
}

// ГЕЙТ: менять только с разрешения пользователя.
/** Снимок и момент, когда мы его получили: по нему решаем, пора ли обновлять (`date` — дата курсов, а не загрузки). */
class CachedRates(val snapshot: RatesSnapshot, val fetchedAt: Instant)

// ГЕЙТ: менять только с разрешения пользователя.
/** Офлайн-кэш последних курсов. Хранит один снимок; `load` возвращает null, если кэша нет или он битый. */
interface RatesCache {
    suspend fun load(): CachedRates?
    suspend fun save(rates: CachedRates)
}

// ГЕЙТ: менять только с разрешения пользователя.
/**
 * Что видит UI. [cached] — последние известные курсы (null — ещё нет ни кэша, ни загрузки).
 * [refreshing] — идёт чтение кэша или загрузка (в начале true, пока не закончится первая попытка).
 * [failed] — последняя загрузка не удалась; [cached], если есть, остаётся.
 */
data class RatesState(val cached: CachedRates?, val refreshing: Boolean, val failed: Boolean)

// ГЕЙТ: менять только с разрешения пользователя.
/** Кэш + обновление поверх [RatesProvider]/[RatesCache]. Ни один метод не бросает: сбой уходит в [RatesState.failed]. */
interface RatesRepository {
    val state: StateFlow<RatesState>

    /** Читает кэш (в первый раз) и грузит курсы, если кэша нет или он старше суток. На старте и в onStart. */
    suspend fun refreshIfStale()

    /** Грузит курсы принудительно. */
    suspend fun refresh()
}
