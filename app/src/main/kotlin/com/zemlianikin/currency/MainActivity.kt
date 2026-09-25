package com.zemlianikin.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zemlianikin.currency.calc.defaultCalculator
import com.zemlianikin.currency.core.DecayingFrecency
import com.zemlianikin.currency.core.RateTable
import com.zemlianikin.currency.core.localeSeed
import com.zemlianikin.currency.data.PrefsUsageStore
import com.zemlianikin.currency.rates.RatesRepository
import com.zemlianikin.currency.ui.CalculatorScreen
import com.zemlianikin.currency.ui.CurrencyTheme
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CurrencyApp
        setContent {
            CurrencyTheme {
                App(app.ratesRepository)
            }
        }
    }

    // На старте и при каждом возврате в приложение (#15); сам репозиторий решает, устарели ли курсы.
    override fun onStart() {
        super.onStart()
        (application as CurrencyApp).refreshRatesIfStale()
    }
}

@Composable
fun App(repository: RatesRepository) {
    val ratesState by repository.state.collectAsState()
    val snapshot = ratesState.cached?.snapshot
    // Курсов ещё нет — пустая таблица: калькулятор даст NoRate, а экран по ratesState покажет загрузку.
    val rates = remember(snapshot) { snapshot?.toTable() ?: RateTable(LocalDate.now(), emptyMap()) }
    val calculator = remember { defaultCalculator() }
    val context = LocalContext.current.applicationContext
    // Список валют frecency — из снимка; фиксируется при создании, поэтому ключ remember — набор валют:
    // пока курсов нет, он пуст, а когда снимок пришёл (или в нём изменился состав), frecency пересоздаётся.
    val currencies = remember(snapshot) { snapshot?.currencies.orEmpty().toSet() }
    // PoC: стор на SharedPreferences, а не Room (#2).
    val frecency = remember(currencies) {
        DecayingFrecency(PrefsUsageStore(context), currencies.toList(), localeSeed(Locale.getDefault()))
    }
    CalculatorScreen(calculator, rates, ratesState, frecency)
}
