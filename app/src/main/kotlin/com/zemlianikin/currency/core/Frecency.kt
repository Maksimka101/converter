package com.zemlianikin.currency.core

import java.time.Instant

// ГЕЙТ: менять только с разрешения пользователя.
/** Только хранение счётчиков, без логики счёта. */
interface CurrencyUsageStore {
    suspend fun load(): Map<CurrencyCode, CurrencyUsage>
    suspend fun save(changes: Map<CurrencyCode, CurrencyUsage>)
}

// ГЕЙТ: менять только с разрешения пользователя.
/** score на момент [updatedAt]; распад ленивый, считается при чтении и записи. */
data class CurrencyUsage(val score: Double, val updatedAt: Instant)
