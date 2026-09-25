package com.zemlianikin.currency.ui

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zemlianikin.currency.R
import com.zemlianikin.currency.calc.CalcError
import com.zemlianikin.currency.calc.Calculation
import com.zemlianikin.currency.calc.Calculator
import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.CurrencyFrecency
import com.zemlianikin.currency.core.Num
import com.zemlianikin.currency.core.RateTable
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * PoC: поле ввода сверху, результат под ним, ниже — та же сумма во всех остальных валютах в порядке frecency.
 * Пересчёт при каждом изменении текста или курсов. Отступление от #8 (ради PoC): рейтинг не замораживается,
 * а валюты засчитываются сразу, как только во вводе появился новый набор.
 */
@OptIn(FlowPreview::class)
@Composable
fun CalculatorScreen(calculator: Calculator, rates: RateTable, frecency: CurrencyFrecency) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val text = input.text
    // Курсов ещё нет, но грузим: NoRate тут не ошибка ввода (#15).
    val result = remember(text, rates) { calculator.calculate(text, rates) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // Режим numpad (#13): свои кнопки вместо системной клавиатуры, последний режим запоминается.
    val prefs = LocalContext.current.getSharedPreferences("ui", Context.MODE_PRIVATE)
    var numpad by rememberSaveable { mutableStateOf(prefs.getBoolean("numpad", false)) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(numpad) {
        runCatching { focus.requestFocus() } // поле пересоздаётся при смене режима
        if (numpad) keyboard?.hide() else keyboard?.show()
    }

    // Экран не прыгает при смене режима (#13): в numpad вместо клавиатуры блок её высоты, а пока клавиатура
    // выезжает обратно, место под неё уже зарезервировано.
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
    val safeBottom = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    val bottomInsets = when {
        numpad -> WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
        keyboardComing -> safeBottom.union(WindowInsets(bottom = keyboardHeight.px))
        else -> safeBottom
    }
    val numpadHeight = if (keyboardHeight.px == 0) null else with(density) {
        (keyboardHeight.px - WindowInsets.systemBars.getBottom(density)).toDp().coerceAtLeast(240.dp)
    }

    var ranking by remember { mutableStateOf(emptyList<CurrencyCode>()) }
    LaunchedEffect(frecency) { ranking = frecency.ranking(Instant.now()) }

    // Один набор валют засчитывается один раз, пока ввод не очистят.
    var recorded by remember { mutableStateOf<Set<CurrencyCode>?>(null) }
    // Последний валидный результат и выражение, из которого он получен (#11): пока ввод невалиден, показываем
    // его тусклым, а копирование берёт именно это выражение. Пустой ввод сбрасывает.
    var lastOk by remember { mutableStateOf<Shown?>(null) }
    val ok = result as? Calculation.Ok
    val shown = when {
        text.isEmpty() -> null
        ok != null -> Shown(text, ok)
        else -> lastOk
    }
    LaunchedEffect(shown) { lastOk = shown }

    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val copy = { value: String, feedback: HapticFeedbackType ->
        haptic.performHapticFeedback(feedback)
        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, value))) }
        Unit
    }

    val used = (result as? Calculation.Ok)?.currencies.orEmpty()
    LaunchedEffect(used, text.isEmpty()) {
        if (text.isEmpty()) recorded = null
        else if (used.isNotEmpty() && used != recorded) {
            recorded = used
            val now = Instant.now()
            frecency.recordUsed(used, now)
            ranking = frecency.ranking(now)
        }
    }

    val suggestions = remember(input, result, ranking, numpad) {
        if (numpad) suggestCurrencies(input, result, ranking) else Suggestions.None
    }

    // Кнопка «+» рядом с чипами (#13): валюта по названию или стране вписывается в ввод и засчитывается во frecency.
    val directory = remember { CurrencyDirectory() }
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        CurrencyPickerDialog(
            directory,
            ranking,
            onPick = { code ->
                picking = false
                input = applySuggestion(input, pickRange(input, suggestions), code, calculator, rates)
                scope.launch {
                    val now = Instant.now()
                    frecency.recordUsed(setOf(code), now)
                    ranking = frecency.ranking(now)
                }
            },
            onDismiss = { picking = false },
        )
    }

    Surface(Modifier.fillMaxSize()) {
        // Снизу вверх по ходу руки: ввод и кнопки у клавиатуры, над ними результат, выше — список валют.
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .windowInsetsPadding(bottomInsets)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
        ) {
            Text(
                stringResource(R.string.rates_footer, rates.date.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Conversions(result, shown, ranking, rates, copy, Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            ExpressionField(
                value = input,
                onValueChange = { input = editInput(input, it, calculator, rates) },
                visualTransformation = underlineError(result, MaterialTheme.colorScheme.error),
                onClear = { input = TextFieldValue() },
                numpad = numpad,
                onToggleMode = {
                    numpad = !numpad
                    keyboardComing = !numpad
                    prefs.edit().putBoolean("numpad", numpad).apply()
                },
                modifier = Modifier.focusRequester(focus),
            )
            if (numpad) {
                CurrencyChips(
                    suggestions.codes,
                    onPick = { input = applySuggestion(input, suggestions.replace, it, calculator, rates) },
                    onAdd = { picking = true },
                )
                Numpad(
                    onKey = { input = typeInput(input, it, calculator, rates) },
                    onBackspace = { input = deleteInput(input, calculator, rates) },
                    decimal = DecimalFormatSymbols(java.util.Locale.getDefault()).decimalSeparator.toString(),
                    height = numpadHeight,
                )
            } else {
                OperatorKeys(KEYS, onKey = { input = typeInput(input, it, calculator, rates) })
            }
        }
    }
}

/** Кнопка → набираемый текст. Ряд над системной клавиатурой, как extra-keys в Termux (#13). */
private val KEYS = listOf("+" to "+", "−" to "-", "×" to "*", "÷" to "/", "(" to "(", ")" to ")", "%" to "%", "to" to "to")

/**
 * Сумма во всех валютах рейтинга, кроме валюты главной строки; безразмерный результат — без списка (#5).
 * Главный результат — последняя карточка того же wrap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Conversions(
    result: Calculation,
    shown: Shown?,
    ranking: List<CurrencyCode>,
    rates: RateTable,
    copy: (String, HapticFeedbackType) -> Unit,
    modifier: Modifier,
) {
    val money = shown?.ok?.value as? Value.Money
    val rows = if (money == null) emptyList() else ranking.mapNotNull { code ->
        if (code == money.currency) return@mapNotNull null
        val rate = rates.rate(money.currency, code) ?: return@mapNotNull null
        Value.Money(Num(money.amount.value * rate.value), code)
    }
    // Карточки переносятся по строкам и прижаты к правому нижнему углу: самая частая валюта перед результатом,
    // сам результат последний, у большого пальца. Не влезло — прокрутка, начало снизу.
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        FlowRow(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in rows.asReversed()) ConversionCard(row.currency.code, format(row.amount.value, 2))
            ResultText(result, shown, copy)
        }
    }
}

/**
 * Главный результат. Тап копирует его, долгий тап — «выражение = результат». Ввод невалиден — показываем последний
 * валидный тусклым; если валидного ещё не было, текст ошибки (неполный ввод не показывает ничего).
 */
@Composable
private fun ResultText(
    result: Calculation,
    shown: Shown?,
    copy: (String, HapticFeedbackType) -> Unit,
) {
    when {
        shown != null -> {
            val value = formatValue(shown.ok.value)
            ResultCard(
                value,
                isError = false,
                dim = result !is Calculation.Ok,
                onClick = { copy(value, HapticFeedbackType.Confirm) },
                onLongClick = { copy("${shown.expression} = $value", HapticFeedbackType.LongPress) },
                clickLabel = stringResource(R.string.copy_result),
                longClickLabel = stringResource(R.string.copy_with_expression),
            )
        }
        result is Calculation.Failed -> ResultCard(errorText(result.error), isError = true)
    }
}

/** Валидный результат и выражение, из которого он получен. */
private data class Shown(val expression: String, val ok: Calculation.Ok)

/** Строка над списком (#15): дата курсов, загрузка или сбой. */

@Composable
private fun errorText(error: CalcError): String = when (error) {
    CalcError.UnknownWord -> stringResource(R.string.error_unknown_word)
    is CalcError.AmbiguousCurrency ->
        stringResource(R.string.error_ambiguous_currency, error.options.joinToString("/") { it.code })
    CalcError.BadNumber -> stringResource(R.string.error_bad_number)
    CalcError.UnexpectedToken -> stringResource(R.string.error_unexpected_token)
    CalcError.MissingOperator -> stringResource(R.string.error_missing_operator)
    CalcError.TwoCurrencies -> stringResource(R.string.error_two_currencies)
    CalcError.MixedNumberMoney -> stringResource(R.string.error_mixed_number_money)
    CalcError.MoneyTimesMoney -> stringResource(R.string.error_money_times_money)
    CalcError.DivideByMoney -> stringResource(R.string.error_divide_by_money)
    CalcError.MoneyPercent -> stringResource(R.string.error_money_percent)
    CalcError.DivideByZero -> stringResource(R.string.error_divide_by_zero)
    CalcError.ConvertRatio -> stringResource(R.string.error_convert_ratio)
    CalcError.NoRate -> stringResource(R.string.error_no_rate)
}

// Форматирование временное: точность и локаль решаются в #5.
private fun formatValue(value: Value): String = when (value) {
    is Value.Money -> "${format(value.amount.value, 2)} ${value.currency.code}"
    is Value.Number -> format(value.value.value, 8)
    is Value.Ratio -> {
        val percent = value.value.value.subtract(BigDecimal.ONE).multiply(BigDecimal(100))
        "×${format(value.value.value, 4)} (${if (percent.signum() >= 0) "+" else ""}${format(percent, 1)}%)"
    }
}

private fun format(number: BigDecimal, maxFraction: Int): String {
    val minFraction = if (maxFraction == 2) 2 else 0
    val pattern = "#,##0." + "0".repeat(minFraction) + "#".repeat(maxFraction - minFraction)
    return DecimalFormat(pattern, DecimalFormatSymbols(java.util.Locale.getDefault())).format(number)
}
