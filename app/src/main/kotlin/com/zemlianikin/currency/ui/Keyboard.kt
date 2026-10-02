package com.zemlianikin.currency.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
private fun uiPrefs(): SharedPreferences = LocalContext.current.getSharedPreferences("ui", Context.MODE_PRIVATE)

private const val NUMPAD_PREF = "numpad"

/**
 * Режим ввода и место под клавиатуру в текущем кадре. [numpad] — свои кнопки вместо системной клавиатуры,
 * [toggle] меняет режим, последний запоминается. [bottomInsets] — нижний отступ экрана, [numpadHeight] — высота
 * блока numpad (`null`, пока высоту системной клавиатуры не видели).
 */
class KeyboardLayout(
    val numpad: Boolean,
    val bottomInsets: WindowInsets,
    val numpadHeight: Dp?,
    val toggle: () -> Unit,
)

/**
 * Экран не прыгает при смене режима: в numpad вместо клавиатуры блок её высоты, а пока клавиатура
 * выезжает обратно, место под неё уже зарезервировано.
 */
@OptIn(FlowPreview::class)
@Composable
fun keyboardLayout(): KeyboardLayout {
    val prefs = uiPrefs()
    var numpad by remember { mutableStateOf(prefs.getBoolean(NUMPAD_PREF, false)) }
    val keyboardHeight = rememberKeyboardHeight()
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var keyboardComing by remember { mutableStateOf(false) }
    LaunchedEffect(keyboardComing) {
        if (keyboardComing) {
            // Снимаем резерв, когда IME дорос до запомненной высоты (отступ тогда не меняется) или встал
            // на месте (высота изменилась). Раньше нельзя: отступ просел бы, а потом снова вырос.
            val full = keyboardHeight.px - 2
            withTimeoutOrNull(1500) {
                snapshotFlow { ime.getBottom(density) }.debounce { if (it >= full) 0L else 200L }.first { it > 0 }
            }
            keyboardComing = false
        }
    }
    // Edge-to-edge: отступаем от системной полоски навигации (navigationBars), а фон под ней всё равно
    // рисует на весь экран Surface экрана — окно не ужимается, просто контент не наезжает на полоску.
    val bar = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    val safeBottom = bar.union(WindowInsets.ime).union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom)
    val bottomInsets = when {
        numpad -> bar
        keyboardComing -> safeBottom.union(WindowInsets(bottom = keyboardHeight.px))
        else -> safeBottom
    }
    val numpadHeight = if (keyboardHeight.px == 0) null else with(density) {
        (keyboardHeight.px - bar.getBottom(density)).toDp().coerceAtLeast(240.dp)
    }
    return KeyboardLayout(numpad, bottomInsets, numpadHeight, toggle = {
        numpad = !numpad
        keyboardComing = !numpad
        prefs.edit().putBoolean(NUMPAD_PREF, numpad).apply()
    })
}

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
private fun rememberKeyboardHeight(): KeyboardHeight {
    val prefs = uiPrefs()
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
