package com.zemlianikin.currency.data

import java.util.prefs.Preferences

/** [KeyValueStore] на `java.util.prefs`: реестр в Windows, plist в macOS, `~/.java` в Linux. */
class JavaPrefsStore(name: String) : KeyValueStore {
    private val prefs = Preferences.userRoot().node("com/zemlianikin/currency/$name")

    override fun all(): Map<String, String> = prefs.keys().mapNotNull { key -> prefs.get(key, null)?.let { key to it } }.toMap()

    override fun get(key: String): String? = prefs.get(key, null)

    override fun put(values: Map<String, String>) {
        values.forEach { (key, value) -> prefs.put(key, value) }
    }
}
