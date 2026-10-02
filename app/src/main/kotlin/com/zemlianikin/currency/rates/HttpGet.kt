package com.zemlianikin.currency.rates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TIMEOUT_MS = 10_000
private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024

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
