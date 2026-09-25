package com.zemlianikin.currency.rates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Основной URL: CDN jsDelivr. */
const val FAWAZ_PRIMARY_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"

/** Запасной URL: зеркало того же источника на Cloudflare Pages. */
const val FAWAZ_FALLBACK_URL = "https://latest.currency-api.pages.dev/v1/currencies/usd.json"

private const val TIMEOUT_MS = 10_000
private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024

/**
 * Реальный источник курсов (#15): fawazahmed0/exchange-api. Пробует [urls] по порядку;
 * если упали все — бросает исключение последней ошибки. Получение текста вынесено в [get],
 * чтобы провайдер проверялся без сети.
 */
class FawazRatesProvider(
    private val urls: List<String> = listOf(FAWAZ_PRIMARY_URL, FAWAZ_FALLBACK_URL),
    private val get: suspend (String) -> String = ::httpGet,
) : RatesProvider {
    init {
        require(urls.isNotEmpty()) { "Нужен хотя бы один URL" }
    }

    override suspend fun fetch(): RatesSnapshot {
        var last: Exception? = null
        for (url in urls) {
            try {
                return parseRates(get(url))
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
            }
        }
        throw last!!
    }
}

/** GET через [HttpURLConnection] в IO-диспетчере. HTTP != 200 и ответ больше 4 МБ — ошибка. */
suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code для $url")
        val out = ByteArrayOutputStream()
        conn.inputStream.use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_RESPONSE_BYTES) throw IOException("Ответ больше $MAX_RESPONSE_BYTES байт: $url")
            }
        }
        out.toString(Charsets.UTF_8)
    } finally {
        conn.disconnect()
    }
}
