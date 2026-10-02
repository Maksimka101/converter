package com.zemlianikin.currency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.zemlianikin.currency.calc.defaultCalculator
import com.zemlianikin.currency.core.DecayingFrecency
import com.zemlianikin.currency.core.RateTable
import com.zemlianikin.currency.core.today
import com.zemlianikin.currency.data.KeyValueStore
import com.zemlianikin.currency.data.KeyValueUsageStore
import com.zemlianikin.currency.data.localeSeed
import com.zemlianikin.currency.rates.RatesRepository
import com.zemlianikin.currency.ui.CalculatorScreen
import com.zemlianikin.currency.ui.CurrencyTheme

/**
 * Приложение целиком. Платформа даёт то, что живёт дольше экрана: репозиторий курсов и хранилища настроек —
 * [usagePrefs] для frecency, [uiPrefs] для режима клавиатуры.
 */
@Composable
fun App(repository: RatesRepository, usagePrefs: KeyValueStore, uiPrefs: KeyValueStore) {
    CurrencyTheme {
        val ratesState by repository.state.collectAsState()
        val snapshot = ratesState.cached?.snapshot
        // Курсов ещё нет — пустая таблица: калькулятор даст NoRate, а экран по ratesState покажет загрузку.
        val rates = remember(snapshot) { snapshot?.toTable() ?: RateTable(today(), emptyMap()) }
        val calculator = remember { defaultCalculator() }
        // Список валют frecency — из снимка; фиксируется при создании, поэтому ключ remember — набор валют:
        // пока курсов нет, он пуст, а когда снимок пришёл (или в нём изменился состав), frecency пересоздаётся.
        val currencies = remember(snapshot) { snapshot?.currencies.orEmpty().toSet() }
        val frecency = remember(currencies) {
            DecayingFrecency(KeyValueUsageStore(usagePrefs), currencies.toList(), localeSeed())
        }
        CalculatorScreen(calculator, rates, ratesState, frecency, uiPrefs)
    }
}
