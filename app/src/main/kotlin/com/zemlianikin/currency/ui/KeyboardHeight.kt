package com.zemlianikin.currency.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * Последняя высота системной клавиатуры в px от нижнего края экрана (вместе с навбаром), [px] = 0 — ещё не
 * видели. Numpad занимает ровно это место и режимы переключаются без сдвига остального экрана.
 */
@Stable
class KeyboardHeight internal constructor(private val prefs: SharedPreferences, private val key: String) {
    var px by mutableIntStateOf(prefs.getInt(key, 0))
        private set

    internal fun remember(value: Int) {
        if (value <= 0 || value == px) return
        px = value
        prefs.edit().putInt(key, value).apply()
    }
}

/**
 * Следит за IME и хранит установившуюся высоту (без промежуточных кадров анимации). Значение своё для каждого
 * размера окна: у сложенного и раскрытого экрана, портрета и альбома клавиатура разная.
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberKeyboardHeight(): KeyboardHeight {
    val prefs = LocalContext.current.getSharedPreferences("ui", Context.MODE_PRIVATE)
    val config = LocalConfiguration.current
    val key = "keyboard_${config.screenWidthDp}x${config.screenHeightDp}"
    val height = remember(key) { KeyboardHeight(prefs, key) }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    LaunchedEffect(height) {
        snapshotFlow { ime.getBottom(density) }.debounce(300).collect { height.remember(it) }
    }
    return height
}
