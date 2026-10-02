package com.zemlianikin.currency.data

import android.content.Context

/** [KeyValueStore] на SharedPreferences. Значения, записанные раньше как Boolean и Int, читаются строкой. */
class SharedPrefsStore(context: Context, name: String) : KeyValueStore {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun all(): Map<String, String> = prefs.all.mapNotNull { (key, value) -> value?.let { key to it.toString() } }.toMap()

    override fun get(key: String): String? = prefs.all[key]?.toString()

    override fun put(values: Map<String, String>) {
        prefs.edit().apply {
            // remove перед putString: тип старого значения мог быть другим.
            values.forEach { (key, value) -> remove(key).putString(key, value) }
        }.apply()
    }
}
