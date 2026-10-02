package com.zemlianikin.currency.spike

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual val platformName: String = "Android"

actual fun plainTextEntry(text: String): ClipEntry = ClipEntry(ClipData.newPlainText(null, text))

val decimals: Decimals = JvmDecimals()
