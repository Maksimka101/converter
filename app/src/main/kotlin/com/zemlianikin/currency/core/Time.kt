package com.zemlianikin.currency.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Сегодняшняя дата в поясе устройства. */
fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
