package com.zemlianikin.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zemlianikin.currency.data.SharedPrefsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CurrencyApp
        val usagePrefs = SharedPrefsStore(app, "frecency")
        val uiPrefs = SharedPrefsStore(app, "ui")
        setContent {
            App(app.ratesRepository, usagePrefs, uiPrefs)
        }
    }

    // На старте и при каждом возврате в приложение; сам репозиторий решает, устарели ли курсы.
    override fun onStart() {
        super.onStart()
        (application as CurrencyApp).refreshRatesIfStale()
    }
}
