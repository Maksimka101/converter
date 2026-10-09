package com.zemlianikin.currency.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.input.KeyboardType

// То, что в Compose на платформах устроено по-разному.

/** Есть экранная клавиатура. Без неё numpad и его переключатель не нужны. */
expect val hasScreenKeyboard: Boolean

/** Тип системной клавиатуры у поля ввода выражения: буквы и цифры без подсказок и автозамены. */
expect val expressionKeyboardType: KeyboardType

/** Текст для буфера обмена. */
expect fun plainTextEntry(text: String): ClipEntry

/** Цвета темы: на Android 12+ — из обоев, иначе стандартные. */
@Composable
expect fun platformColorScheme(dark: Boolean): ColorScheme

/** Ключ размера окна: высота клавиатуры запоминается отдельно для каждого. */
@Composable
expect fun windowSizeKey(): String

/**
 * Отступы от системных полосок, скруглений и вырезов, о которых не знают `WindowInsets` Compose: в браузере
 * они всегда нулевые. На Android и desktop добавлять нечего.
 */
@Composable
expect fun extraSafeArea(): WindowInsets
