package com.zemlianikin.currency.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zemlianikin.currency.R
import com.zemlianikin.currency.core.CurrencyCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Kind { Digit, Operator, Word, Backspace }

/** Клавиша: подпись, набираемый текст (`null` — backspace) и вид. */
private class Key(val label: String, val text: String?, val kind: Kind)

private fun digit(c: Char) = Key(c.toString(), c.toString(), Kind.Digit)
private fun op(label: String, text: String = label) = Key(label, text, Kind.Operator)

/**
 * Экранный numpad вместо системной клавиатуры (#13). Нажатие срабатывает сразу при касании, `⌫` повторяется
 * при удержании. Только вид: набор и удаление делают [onKey] и [onBackspace]. [decimal] — разделитель дроби локали.
 * [height] — высота всего блока (равна высоте системной клавиатуры, чтобы режимы не сдвигали экран): кнопки
 * растягиваются до неё; `null` — естественная высота.
 */
@Composable
fun Numpad(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    decimal: String,
    height: Dp?,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf(op("("), op(")"), op("%"), Key("⌫", null, Kind.Backspace)),
        listOf(digit('7'), digit('8'), digit('9'), op("÷", "/")),
        listOf(digit('4'), digit('5'), digit('6'), op("×", "*")),
        listOf(digit('1'), digit('2'), digit('3'), op("−", "-")),
        listOf(digit('0'), Key(decimal, decimal, Kind.Digit), Key("to", "to", Kind.Word), op("+")),
    )
    val sized = if (height != null) modifier.height(height) else modifier
    Column(sized.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in rows) {
            Row(
                if (height != null) Modifier.weight(1f) else Modifier,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (key in row) {
                    NumpadKey(key, stretch = height != null, onPress = { key.text?.let(onKey) ?: onBackspace() })
                }
            }
        }
    }
}

@Composable
private fun RowScope.NumpadKey(key: Key, stretch: Boolean, onPress: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (key.kind) {
        Kind.Digit -> colors.surfaceContainerHigh to colors.onSurface
        Kind.Operator -> colors.secondaryContainer to colors.onSecondaryContainer
        Kind.Word -> colors.primary to colors.onPrimary
        Kind.Backspace -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 28.dp else 18.dp, Motion.fastSpatial())
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onPress)
    Surface(
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = Modifier
            .weight(1f)
            .then(if (stretch) Modifier.fillMaxHeight() else Modifier.height(56.dp))
            .semantics { role = Role.Button; onClick { current(); true } }
            .pointerInput(key) {
                detectTapGestures(onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interaction.emit(press)
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    current()
                    val repeat = if (key.kind == Kind.Backspace) scope.launch {
                        delay(400)
                        while (true) {
                            current()
                            delay(60)
                        }
                    } else null
                    val released = tryAwaitRelease()
                    repeat?.cancel()
                    interaction.emit(if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                })
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(key.label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Чипы валют над numpad: тап выбирает валюту. Показаны все [codes], а не только подходящие к месту курсора:
 * чипы вне [enabled] неактивны, чтобы ряд не мерцал при наборе. Высота фиксирована (68 dp, как у ряда [OperatorKeys]), чтобы
 * поле ввода не сдвигалось ни когда чипов нет, ни при смене режима.
 */
@Composable
fun CurrencyChips(
    codes: List<CurrencyCode>,
    enabled: Set<CurrencyCode>,
    onPick: (CurrencyCode) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().height(68.dp), verticalAlignment = Alignment.CenterVertically) {
        // Кнопка «+» слева: чипы важнее и остаются у большого пальца.
        val add = stringResource(R.string.add_currency)
        FilledTonalIconButton(onClick = onAdd, modifier = Modifier.semantics { contentDescription = add }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        // reverseLayout + Alignment.End: чипы прижаты к правому краю, самая частая валюта у большого пальца.
        // Своё arrangement перекрывает умолчание reverseLayout (End), поэтому выравнивание задано явно.
        LazyRow(
            Modifier.weight(1f),
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(codes, key = { it.code }) { code ->
                SuggestionChip(
                    onClick = { onPick(code) },
                    enabled = code in enabled,
                    label = { Text(code.code, style = MaterialTheme.typography.titleMedium) },
                    shape = CircleShape,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f),
                    ),
                    border = null,
                )
            }
        }
    }
}
