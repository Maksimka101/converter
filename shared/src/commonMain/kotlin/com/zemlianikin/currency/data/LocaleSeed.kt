package com.zemlianikin.currency.data

import com.zemlianikin.currency.core.CurrencyCode

/** Валюты для первого запуска по локали устройства: местная, затем USD, EUR. */
expect fun localeSeed(): List<CurrencyCode>
