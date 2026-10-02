package com.zemlianikin.currency.ui

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zemlianikin.currency.R
import com.zemlianikin.currency.calc.CalcError
import com.zemlianikin.currency.calc.Calculation
import com.zemlianikin.currency.calc.Calculator
import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.DecayingFrecency
import com.zemlianikin.currency.core.RateTable
import com.zemlianikin.currency.core.today
import com.zemlianikin.currency.rates.RatesState
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Экран калькулятора, снизу вверх по ходу руки: ввод и кнопки у клавиатуры, над ними результат, выше — та же сумма
 * в остальных валютах в порядке frecency, сверху строка о курсах. Здесь только раскладка: состояние — в
 * [CalculatorState], режим клавиатуры и место под неё — в [keyboardLayout].
 * [ratesState] — откуда курсы и что с ними: по нему строка о курсах и «загружаем курсы» вместо ошибки «нет курса».
 */
@Composable
fun CalculatorScreen(calculator: Calculator, rates: RateTable, ratesState: RatesState, frecency: DecayingFrecency) {
    val state = rememberCalculatorState(calculator, rates, frecency)
    val keyboard = keyboardLayout()
    val format = remember { ValueFormatter() }
    // Курсов ещё нет, но грузим: NoRate тут не ошибка ввода.
    val loadingRates = ratesState.cached == null && ratesState.refreshing

    val focus = remember { FocusRequester() }
    val systemKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(keyboard.numpad) {
        runCatching { focus.requestFocus() } // поле пересоздаётся при смене режима
        if (keyboard.numpad) systemKeyboard?.hide() else systemKeyboard?.show()
    }

    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val copy = { value: String, feedback: HapticFeedbackType ->
        haptic.performHapticFeedback(feedback)
        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, value))) }
        Unit
    }

    // Кнопка «+» рядом с чипами: валюта по названию или стране вписывается в ввод и засчитывается во frecency.
    val directory = remember { CurrencyDirectory() }
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        CurrencyPickerDialog(
            directory,
            state.ranking,
            onPick = {
                picking = false
                state.pickCurrency(it)
            },
            onDismiss = { picking = false },
        )
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .windowInsetsPadding(keyboard.bottomInsets)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
        ) {
            Text(
                ratesStatus(ratesState),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Conversions(state.result, state.shown, state.ranking, state.rates, loadingRates, format, copy, Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            ExpressionField(
                value = state.input,
                onValueChange = state::edit,
                visualTransformation = underlineError(state.result, MaterialTheme.colorScheme.error),
                onClear = state::clear,
                numpad = keyboard.numpad,
                onToggleMode = keyboard.toggle,
                focusRequester = focus,
            )
            // Ряд над клавиатурой меняется вместе с режимом: чипы валют выезжают снизу, от numpad, а ряд операторов
            // уходит вверх; обратно — наоборот. Ряды одной высоты, поэтому поле ввода стоит на месте.
            AnimatedContent(
                targetState = keyboard.numpad,
                transitionSpec = {
                    val from = if (targetState) 1 else -1
                    (slideInVertically(Motion.spatial()) { it * from } + fadeIn(Motion.effects()))
                        .togetherWith(slideOutVertically(Motion.spatial()) { -it * from } + fadeOut(Motion.effects()))
                },
                label = "keyRow",
            ) { numpad ->
                if (numpad) {
                    // Ряд валют не прыгает: остаются все чипы топа, неподходящие к месту курсора неактивны.
                    val ranking = state.ranking
                    val suggestions = state.suggestions
                    val chips = remember(ranking, suggestions) { (ranking.take(CHIP_LIMIT) + suggestions.codes).distinct() }
                    CurrencyChips(
                        chips,
                        enabled = suggestions.codes.toSet(),
                        onPick = state::pickSuggestion,
                        onAdd = { picking = true },
                    )
                } else {
                    OperatorKeys(onKey = state::type)
                }
            }
            if (keyboard.numpad) {
                Numpad(
                    onKey = state::type,
                    onBackspace = state::backspace,
                    decimal = format.decimalSeparator,
                    height = keyboard.numpadHeight,
                )
            }
        }
    }
}

/** Сколько карточек конвертаций считается и рисуется сразу и на сколько больше — при подходе к краю прокрутки. */
private const val CARDS_PAGE = 12
private const val CARDS_LOAD_MARGIN_PX = 400

/**
 * Сумма в валютах рейтинга (ленивая подгрузка по прокрутке), кроме валюты главной строки; безразмерный результат — без списка.
 * Главный результат — последняя карточка того же wrap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Conversions(
    result: Calculation,
    shown: Shown?,
    ranking: List<CurrencyCode>,
    rates: RateTable,
    loadingRates: Boolean,
    format: ValueFormatter,
    copy: (String, HapticFeedbackType) -> Unit,
    modifier: Modifier,
) {
    val money = shown?.ok?.value as? Value.Money
    // В рейтинге сотни валют, считать и рисовать их все на каждое нажатие — лаг. Берём первые [count] по
    // рейтингу, остальные подгружаются, когда прокрутка доходит до края. Пустой ввод сбрасывает подгруженное.
    var count by remember(money == null) { mutableIntStateOf(CARDS_PAGE) }
    val rows = remember(money, ranking, rates, count) {
        if (money == null) emptyList() else conversions(money, ranking, rates).take(count).toList()
    }
    val scroll = rememberScrollState()
    LaunchedEffect(scroll, ranking.size) {
        // reverseScrolling: конец прокрутки — верх списка. Подгружаем, пока до него меньше запаса (в паре с count,
        // чтобы после подгрузки проверка повторилась, если карточек всё ещё мало).
        snapshotFlow { (scroll.maxValue - scroll.value < CARDS_LOAD_MARGIN_PX) to count }.collect { (nearEnd, shownCount) ->
            if (nearEnd && shownCount < ranking.size) count = shownCount + CARDS_PAGE
        }
    }
    // Карточки переносятся по строкам и прижаты к правому нижнему углу: самая частая валюта перед результатом,
    // сам результат последний, у большого пальца. Не влезло — прокрутка, начало снизу.
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        FlowRow(
            Modifier.fillMaxWidth().verticalScroll(scroll, reverseScrolling = true),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in rows.asReversed()) ConversionCard(row.currency.code, format.amount(row.amount))
            ResultText(result, shown, loadingRates, format, copy)
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
    loadingRates: Boolean,
    format: ValueFormatter,
    copy: (String, HapticFeedbackType) -> Unit,
) {
    when {
        shown != null -> {
            val value = format.value(shown.ok.value)
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
        result is Calculation.Failed && result.error == CalcError.NoRate && loadingRates ->
            ResultCard(stringResource(R.string.rates_loading), isError = false, dim = true)
        result is Calculation.Failed -> ResultCard(errorText(result.error), isError = true)
    }
}

/** Строка над списком: дата курсов, загрузка или сбой. */
@Composable
private fun ratesStatus(state: RatesState): String {
    val cached = state.cached
    return when {
        cached == null -> stringResource(if (state.refreshing) R.string.rates_loading else R.string.rates_failed)
        state.failed -> stringResource(R.string.rates_stale, ratesDay(cached.snapshot.date))
        else -> stringResource(R.string.rates_fresh, ratesDay(cached.snapshot.date))
    }
}

/** Дата курсов словом: сегодня, вчера, позавчера; раньше (и «из будущего» при сбитых часах) — датой. */
@Composable
private fun ratesDay(date: LocalDate): String = when (date.daysUntil(today())) {
    0 -> stringResource(R.string.rates_day_today)
    1 -> stringResource(R.string.rates_day_yesterday)
    2 -> stringResource(R.string.rates_day_before_yesterday)
    else -> date.toString()
}

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
