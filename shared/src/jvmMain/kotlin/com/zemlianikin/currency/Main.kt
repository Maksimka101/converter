package com.zemlianikin.currency

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zemlianikin.currency.data.JavaPrefsStore
import com.zemlianikin.currency.rates.CachedRatesRepository
import com.zemlianikin.currency.rates.FawazRatesProvider
import com.zemlianikin.currency.rates.FileRatesCache
import com.zemlianikin.currency.rates.httpGet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

fun main() {
    val repository = CachedRatesRepository(FawazRatesProvider(get = ::httpGet), FileRatesCache(File(cacheDir(), "rates.txt")))
    // Курсы обновляются при запуске; репозиторий сам решает, устарели ли они.
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repository.refreshIfStale() }
    val usagePrefs = JavaPrefsStore("frecency")
    val uiPrefs = JavaPrefsStore("ui")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Converter",
            state = rememberWindowState(width = 420.dp, height = 720.dp),
        ) {
            App(repository, usagePrefs, uiPrefs)
        }
    }
}

/** Каталог кэша приложения по правилам ОС. */
private fun cacheDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    val base = when {
        "win" in os -> System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local"
        "mac" in os -> "$home/Library/Caches"
        else -> System.getenv("XDG_CACHE_HOME") ?: "$home/.cache"
    }
    return File(base, "currency-converter")
}
