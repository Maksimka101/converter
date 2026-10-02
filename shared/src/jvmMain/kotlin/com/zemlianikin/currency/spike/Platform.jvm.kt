package com.zemlianikin.currency.spike

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

actual val platformName: String = "Desktop"

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))

val decimals: Decimals = JvmDecimals()
