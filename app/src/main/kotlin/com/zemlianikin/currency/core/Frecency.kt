package com.zemlianikin.currency.core

import java.time.Instant

// ГЕЙТ: менять только с разрешения пользователя.
/**
 * Частота + давность использования валют: счёт, распад, засев по локали. Про выражение
 * и про топ-N не знает — отдаёт полный рейтинг, исключения и обрезку делает вызывающий.
 *
 * Как пользоваться:
 * - [recordUsed] — один раз на завершённое выражение (копирование, очистка, onStop, пауза ~3 с),
 *   не на каждую правку текста. Брать валюты из последнего валидного `Calculation.Ok`.
 * - [ranking] — на старте и в onStart; результат держать замороженным на время ввода.
 * - Просмотр валюты в топ-N использованием не считается.
 */
interface CurrencyFrecency {
    /** [used] — различные валюты выражения, каждая засчитывается с весом 1. */
    suspend fun recordUsed(used: Set<CurrencyCode>, now: Instant)

    /** Все известные валюты по убыванию score на момент [now]. */
    suspend fun ranking(now: Instant): List<CurrencyCode>
}

// ГЕЙТ: менять только с разрешения пользователя.
/** Только хранение счётчиков, без логики счёта. Реализация — Room. */
interface CurrencyUsageStore {
    suspend fun load(): Map<CurrencyCode, CurrencyUsage>
    suspend fun save(changes: Map<CurrencyCode, CurrencyUsage>)
}

// ГЕЙТ: менять только с разрешения пользователя.
/** score на момент [updatedAt]; распад ленивый, считается при чтении и записи. */
data class CurrencyUsage(val score: Double, val updatedAt: Instant)
