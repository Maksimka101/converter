package com.zemlianikin.currency.ui

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType

actual val hasScreenKeyboard: Boolean = true

// «Видимый пароль», как в Termux: Gboard показывает над буквами ряд цифр (у обычного текста он зависит
// от настройки) и не подсказывает ввод. Обычный Password зовёт менеджер паролей.
actual val expressionKeyboardType: KeyboardType = KeyboardType.PasswordVisible

actual fun plainTextEntry(text: String): ClipEntry = ClipEntry(ClipData.newPlainText(null, text))

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
}

// У сложенного и раскрытого экрана, портрета и альбома клавиатура разная.
@Composable
actual fun windowSizeKey(): String {
    val config = LocalConfiguration.current
    return "${config.screenWidthDp}x${config.screenHeightDp}"
}

@Composable
actual fun extraSafeArea(): WindowInsets = WindowInsets(0)
