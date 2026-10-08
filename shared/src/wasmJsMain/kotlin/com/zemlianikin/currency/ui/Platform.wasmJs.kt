package com.zemlianikin.currency.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardType

private fun coarsePointer(): Boolean = js("window.matchMedia('(pointer: coarse)').matches")

// Основной указатель — палец: значит, телефон или планшет с экранной клавиатурой.
actual val hasScreenKeyboard: Boolean = coarsePointer()

// Парольный тип в браузере превращает поле в поле пароля: над клавиатурой появляется автозаполнение паролей и карт.
actual val expressionKeyboardType: KeyboardType = KeyboardType.Text

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme = if (dark) darkColorScheme() else lightColorScheme()

// Окно браузера меняет размер как угодно, а с ним и экранная клавиатура.
@Composable
actual fun windowSizeKey(): String = LocalWindowInfo.current.containerSize.let { "${it.width}x${it.height}" }
