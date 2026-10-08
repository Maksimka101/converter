package com.zemlianikin.currency

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.zemlianikin.currency.data.LocalStorageStore
import com.zemlianikin.currency.rates.CachedRatesRepository
import com.zemlianikin.currency.rates.FawazRatesProvider
import com.zemlianikin.currency.rates.LocalStorageRatesCache
import com.zemlianikin.currency.rates.httpGet
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val repository = CachedRatesRepository(FawazRatesProvider(get = ::httpGet), LocalStorageRatesCache())
    // Курсы обновляются при открытии страницы; репозиторий сам решает, устарели ли они.
    MainScope().launch { repository.refreshIfStale() }
    val usagePrefs = LocalStorageStore("frecency")
    val uiPrefs = LocalStorageStore("ui")
    ComposeViewport {
        App(repository, usagePrefs, uiPrefs)
    }
}
