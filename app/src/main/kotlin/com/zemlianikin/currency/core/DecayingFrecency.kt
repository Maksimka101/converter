package com.zemlianikin.currency.core

import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Frecency: `score·2^(-Δt/halfLife) + 1` на каждое использование, распад ленивый.
 * Холодный старт: если стор пуст, [seed] записывается со score 1.0 и дальше распадается как обычное использование.
 * Рейтинг — seed-валюты из [known] и все, что когда-либо использовали (список пополняется автоматически):
 * по убыванию score, при равенстве порядок seed, затем порядок записи. Остальные [known] в рейтинг не входят,
 * их добавляет только использование. Про выражение и про топ-N не знает: исключения и обрезку делает вызывающий.
 */
class DecayingFrecency(
    private val store: CurrencyUsageStore,
    private val known: List<CurrencyCode>,
    private val seed: List<CurrencyCode>,
    private val halfLife: Duration = Duration.ofDays(21),
) {
    /** [used] — различные валюты выражения, каждая засчитывается с весом 1. */
    suspend fun recordUsed(used: Set<CurrencyCode>, now: Instant) {
        val usage = loadSeeded(now)
        store.save(used.associateWith { CurrencyUsage(scoreAt(usage[it], now) + 1.0, now) })
    }

    /** Рейтинг по убыванию score на момент [now]. */
    suspend fun ranking(now: Instant): List<CurrencyCode> {
        val usage = loadSeeded(now)
        val base = (seed.filter { it in known } + usage.keys).distinct()
        return base.sortedByDescending { scoreAt(usage[it], now) } // sortedBy стабилен
    }

    private suspend fun loadSeeded(now: Instant): Map<CurrencyCode, CurrencyUsage> {
        val stored = store.load()
        if (stored.isNotEmpty()) return stored
        // Засев вне [known] не пишем: иначе он попал бы в рейтинг вместе с реально использованными валютами.
        val seeded = seed.filter { it in known }.associateWith { CurrencyUsage(1.0, now) }
        store.save(seeded)
        return seeded
    }

    private fun scoreAt(usage: CurrencyUsage?, now: Instant): Double {
        if (usage == null) return 0.0
        val elapsed = Duration.between(usage.updatedAt, now).toMillis().coerceAtLeast(0)
        return usage.score * Math.pow(0.5, elapsed.toDouble() / halfLife.toMillis())
    }
}

/**
 * Дефолты из локали: валюта, затем USD, EUR. Валюта — явная (`-u-cu-`), иначе страна из региона в настройках
 * телефона (`-u-rg-`, Android 14+), иначе страна самой локали.
 */
fun localeSeed(locale: Locale): List<CurrencyCode> {
    val local = try {
        locale.getUnicodeLocaleType("cu")?.uppercase()
            ?: locale.getUnicodeLocaleType("rg")?.take(2)?.let { java.util.Currency.getInstance(Locale("", it)).currencyCode }
            ?: java.util.Currency.getInstance(locale).currencyCode
    } catch (_: IllegalArgumentException) {
        null // у локали нет страны
    }
    return listOfNotNull(local, "USD", "EUR").distinct().map(::CurrencyCode)
}
