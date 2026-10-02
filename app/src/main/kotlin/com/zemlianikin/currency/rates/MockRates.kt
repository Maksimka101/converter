package com.zemlianikin.currency.rates

import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.Num
import com.zemlianikin.currency.core.RateTable
import java.math.BigDecimal
import java.time.LocalDate

/** Мок-курсы: USD, EUR, RUB относительно USD. Цифры выдуманные. */
private val mockPerBase = mapOf(
    CurrencyCode("USD") to Num(BigDecimal.ONE),
    CurrencyCode("EUR") to Num(BigDecimal("0.86")),
    CurrencyCode("RUB") to Num(BigDecimal("82.5")),
)

/** Валюты, для которых в моке есть курс: `RateTable` состав не отдаёт. */
val mockCurrencies: List<CurrencyCode> = mockPerBase.keys.toList()

fun mockRates(): RateTable = RateTable(date = LocalDate.now(), perBase = mockPerBase)
