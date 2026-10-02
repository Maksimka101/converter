package com.zemlianikin.currency.spike

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommonTest {
    @Test
    fun clockWorks() = assertTrue(stamp().epochSeconds > 0)

    @Test
    fun failsWith() {
        assertFailsWith<IllegalStateException> { error("x") }
    }
}
