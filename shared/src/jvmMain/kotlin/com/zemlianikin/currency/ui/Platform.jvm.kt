package com.zemlianikin.currency.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

actual val hasScreenKeyboard: Boolean = false

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme = if (dark) darkColorScheme() else lightColorScheme()

// Экранной клавиатуры на desktop нет — ключ один.
@Composable
actual fun windowSizeKey(): String = "desktop"
