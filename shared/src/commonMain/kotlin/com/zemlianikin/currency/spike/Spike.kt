package com.zemlianikin.currency.spike

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.zemlianikin.currency.shared.Res
import com.zemlianikin.currency.shared.spike_hello
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

expect val platformName: String

expect fun plainTextEntry(text: String): ClipEntry

/** Мост к jvmShared: common видит только интерфейс. */
interface Decimals {
    fun sum(a: String, b: String): String
}

fun stamp(): Instant = Clock.System.now()

@Composable
fun SpikeScreen() {
    // API, на которые опирается текущий UI: проверяем, что они есть в common.
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val ime = WindowInsets.ime
    val kinds = listOf(HapticFeedbackType.Confirm, HapticFeedbackType.VirtualKey, HapticFeedbackType.LongPress)
    MaterialTheme {
        Text(
            stringResource(Res.string.spike_hello, platformName),
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}
