package com.zemlianikin.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zemlianikin.currency.calc.defaultCalculator
import com.zemlianikin.currency.core.DecayingFrecency
import com.zemlianikin.currency.core.localeSeed
import com.zemlianikin.currency.data.PrefsUsageStore
import com.zemlianikin.currency.rates.mockCurrencies
import com.zemlianikin.currency.rates.mockRates
import com.zemlianikin.currency.ui.CalculatorScreen
import com.zemlianikin.currency.ui.CurrencyTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurrencyTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    // PoC: мок-курсы, заменить на реальный источник (#15).
    val rates = remember { mockRates() }
    val calculator = remember { defaultCalculator() }
    val context = LocalContext.current.applicationContext
    // PoC: стор на SharedPreferences, а не Room (#2).
    val frecency = remember {
        DecayingFrecency(PrefsUsageStore(context), mockCurrencies, localeSeed(Locale.getDefault()))
    }
    CalculatorScreen(calculator, rates, frecency)
}
