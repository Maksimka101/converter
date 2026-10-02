package com.zemlianikin.currency.spike

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmTest {
    @Test
    fun jvmSharedIsReachable() = assertEquals("3.3", JvmDecimals().sum("1.1", "2.2").replace(',', '.'))
}
