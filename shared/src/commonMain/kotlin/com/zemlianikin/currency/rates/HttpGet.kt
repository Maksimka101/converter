package com.zemlianikin.currency.rates

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readRemaining
import kotlinx.io.IOException
import kotlinx.io.readString

private const val TIMEOUT_MS = 10_000L
private const val MAX_RESPONSE_BYTES = 4L * 1024 * 1024

// Движок — тот, что подключён зависимостью платформы. Браузер умеет только общий таймаут запроса.
private val client by lazy {
    HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
            requestTimeoutMillis = 3 * TIMEOUT_MS
        }
    }
}

/** GET через Ktor. HTTP != 200 и ответ больше 4 МБ — ошибка. */
suspend fun httpGet(url: String): String =
    client.prepareGet(url) { accept(ContentType.Application.Json) }.execute { response ->
        if (response.status != HttpStatusCode.OK) throw IOException("HTTP ${response.status.value} для $url")
        val body = response.bodyAsChannel().readRemaining(MAX_RESPONSE_BYTES + 1)
        if (body.request(MAX_RESPONSE_BYTES + 1)) throw IOException("Ответ больше $MAX_RESPONSE_BYTES байт: $url")
        body.readString()
    }
