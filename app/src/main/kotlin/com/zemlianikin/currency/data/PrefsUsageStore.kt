package com.zemlianikin.currency.data

import android.content.Context
import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.CurrencyUsage
import com.zemlianikin.currency.core.CurrencyUsageStore
import kotlin.time.Instant

/** Стор frecency на SharedPreferences: `USD` → `score;epochMillis`. */
class PrefsUsageStore(context: Context) : CurrencyUsageStore {
    private val prefs = context.getSharedPreferences("frecency", Context.MODE_PRIVATE)

    override suspend fun load(): Map<CurrencyCode, CurrencyUsage> =
        prefs.all.mapNotNull { (code, raw) ->
            val (score, millis) = (raw as? String)?.split(';')?.takeIf { it.size == 2 } ?: return@mapNotNull null
            CurrencyCode(code) to CurrencyUsage(
                score.toDoubleOrNull() ?: return@mapNotNull null,
                Instant.fromEpochMilliseconds(millis.toLongOrNull() ?: return@mapNotNull null),
            )
        }.toMap()

    override suspend fun save(changes: Map<CurrencyCode, CurrencyUsage>) {
        prefs.edit().apply {
            changes.forEach { (code, usage) -> putString(code.code, "${usage.score};${usage.updatedAt.toEpochMilliseconds()}") }
        }.apply()
    }
}
