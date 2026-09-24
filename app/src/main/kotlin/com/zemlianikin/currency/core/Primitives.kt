package com.zemlianikin.currency.core

import java.math.BigDecimal

// ГЕЙТ: менять только с разрешения пользователя.
/** Десятичное число без потери точности (#6). Округляется только при выводе. */
@JvmInline
value class Num(val value: BigDecimal)

// ГЕЙТ: менять только с разрешения пользователя.
/** Код валюты в верхнем регистре: USD, EUR, BTC. */
@JvmInline
value class CurrencyCode(val code: String)
