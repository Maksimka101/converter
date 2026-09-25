package com.zemlianikin.currency

import android.app.Application
import com.zemlianikin.currency.rates.CachedRatesRepository
import com.zemlianikin.currency.rates.FawazRatesProvider
import com.zemlianikin.currency.rates.FileRatesCache
import com.zemlianikin.currency.rates.RatesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Держит то, что живёт дольше Activity: репозиторий курсов один на процесс (#15), а загрузка не обрывается
 * при повороте экрана и пересоздании Activity.
 */
class CurrencyApp : Application() {
    val ratesRepository: RatesRepository by lazy {
        CachedRatesRepository(FawazRatesProvider(), FileRatesCache(File(filesDir, "rates.txt")))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null

    /** Обновить курсы, если устарели. Вызывается из onStart; пока предыдущий вызов идёт, новый не запускается. */
    fun refreshRatesIfStale() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch { ratesRepository.refreshIfStale() }
    }
}
