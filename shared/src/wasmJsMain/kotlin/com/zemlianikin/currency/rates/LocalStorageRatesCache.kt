package com.zemlianikin.currency.rates

import com.zemlianikin.currency.data.storageGet
import com.zemlianikin.currency.data.storageSet

/** Офлайн-кэш курсов в `localStorage`: один снимок под одним ключом. Формат — в `CachedRatesText.kt`. */
class LocalStorageRatesCache(private val key: String = "rates") : RatesCache {

    override suspend fun load(): CachedRates? = try {
        storageGet(key)?.let { decodeCachedRates(it.trimEnd('\n').lines()) }
    } catch (_: Exception) {
        null
    }

    override suspend fun save(rates: CachedRates) = storageSet(key, encodeCachedRates(rates))
}
