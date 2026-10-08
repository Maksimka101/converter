package com.zemlianikin.currency.calc.engine

import com.zemlianikin.currency.calc.Calculation

actual class Stop actual constructor(actual val outcome: Calculation) : Exception()
