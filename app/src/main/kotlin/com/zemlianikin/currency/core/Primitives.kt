package com.zemlianikin.currency.core

import java.math.BigDecimal

/** Десятичное число без потери точности (#6). Округляется только при выводе. */
@JvmInline
value class Num(val value: BigDecimal)

/** Код валюты в верхнем регистре: USD, EUR, BTC. */
@JvmInline
value class CurrencyCode(val code: String)
