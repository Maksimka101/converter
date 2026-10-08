package com.zemlianikin.currency.data

import com.zemlianikin.currency.core.CurrencyCode

// localStorage бросает, когда хранилище запрещено (приватный режим, настройки браузера): тогда работаем без него.

private fun storageKeys(prefix: String): String =
    js("(() => { try { return Object.keys(localStorage).filter(k => k.startsWith(prefix)).join('\\n') } catch (e) { return '' } })()")

internal fun storageGet(key: String): String? =
    js("(() => { try { return localStorage.getItem(key) } catch (e) { return null } })()")

internal fun storageSet(key: String, value: String): Unit =
    js("(() => { try { localStorage.setItem(key, value) } catch (e) {} })()")

/** [KeyValueStore] на `localStorage`: ключи хранилища [name] лежат под префиксом `name/`. */
class LocalStorageStore(name: String) : KeyValueStore {
    private val prefix = "$name/"

    override fun all(): Map<String, String> =
        storageKeys(prefix).split('\n').filter { it.isNotEmpty() }
            .mapNotNull { key -> storageGet(key)?.let { key.removePrefix(prefix) to it } }.toMap()

    override fun get(key: String): String? = storageGet(prefix + key)

    override fun put(values: Map<String, String>) {
        values.forEach { (key, value) -> storageSet(prefix + key, value) }
    }
}

// Валюту страны браузер не сообщает (в Intl такого нет), а своей таблицы «страна → валюта» пока нет.
actual fun localeSeed(): List<CurrencyCode> = listOf("USD", "EUR").map(::CurrencyCode)
