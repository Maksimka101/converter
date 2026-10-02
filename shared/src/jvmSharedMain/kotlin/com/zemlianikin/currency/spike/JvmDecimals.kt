package com.zemlianikin.currency.spike

import java.io.File
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.text.DecimalFormat

/** Проверка: промежуточный source set видит java.*. */
class JvmDecimals : Decimals {
    override fun sum(a: String, b: String): String = DecimalFormat("#.##").format(BigDecimal(a) + BigDecimal(b))
}

fun cacheFile(dir: File): File = File(dir, "rates.txt")

val okCode: Int = HttpURLConnection.HTTP_OK
