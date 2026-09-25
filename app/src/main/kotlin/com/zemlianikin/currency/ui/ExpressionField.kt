package com.zemlianikin.currency.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import com.zemlianikin.currency.R

/**
 * Поле ввода выражения: только внешний вид. Правки текста и подчёркивание ошибки приходят снаружи
 * ([onValueChange], [visualTransformation]), клавиатурные настройки из #13 не менялись.
 */
@Composable
fun ExpressionField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    visualTransformation: VisualTransformation,
    onClear: () -> Unit,
    numpad: Boolean,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        Motion.effects(),
    )
    val textStyle = MaterialTheme.typography.headlineMedium
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().border(2.dp, border, MaterialTheme.shapes.extraLarge),
    ) {
        // Справа меньше, чем слева: у кнопки 48 dp области касания, подпись внутри по центру добавляет ещё ~10 dp.
        Row(Modifier.padding(start = 20.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            // В numpad поле остаётся обычным (иначе не рисуется каретка), но системный ввод к нему не подключается.
            // Сессия ввода живёт, пока поле в композиции, поэтому при смене режима поле пересоздаётся.
            key(numpad) {
                WithoutSystemKeyboard(numpad) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        interactionSource = interaction,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            // «Видимый пароль», как в Termux: Gboard показывает над буквами ряд цифр (у обычного текста
                            // он зависит от настройки) и не подсказывает ввод. Обычный Password зовёт менеджер паролей.
                            keyboardType = KeyboardType.PasswordVisible,
                            showKeyboardOnFocus = !numpad,
                        ),
                        visualTransformation = visualTransformation,
                        decorationBox = { inner ->
                            Box(Modifier.padding(vertical = 20.dp), contentAlignment = Alignment.CenterStart) {
                                if (value.text.isEmpty()) {
                                    Text(
                                        stringResource(R.string.input_hint),
                                        style = textStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        maxLines = 1,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = modifier.weight(1f),
                    )
                }
            }
            if (value.text.isNotEmpty()) {
                val clear = stringResource(R.string.clear)
                IconButton(onClick = onClear, modifier = Modifier.semantics { contentDescription = clear }) {
                    Text("✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val toggle = stringResource(if (numpad) R.string.mode_text else R.string.mode_numpad)
            IconButton(onClick = onToggleMode, modifier = Modifier.semantics { contentDescription = toggle }) {
                Text(if (numpad) "ABC" else "123", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** При [block] ввод с системной клавиатуры к полю не подключается: она не показывается, каретка и выделение работают. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WithoutSystemKeyboard(block: Boolean, content: @Composable () -> Unit) {
    if (block) InterceptPlatformTextInput({ _, _ -> awaitCancellation() }, content) else content()
}
