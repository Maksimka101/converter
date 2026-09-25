package com.zemlianikin.currency.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * PoC: поле ввода сверху, результат под ним, ниже — та же сумма во всех остальных валютах в порядке frecency.
 * Пересчёт при каждом изменении текста или курсов. Отступление от #8 (ради PoC): рейтинг не замораживается,
 * а валюты засчитываются сразу, как только во вводе появился новый набор.
 */
@Composable
fun CalculatorScreen(calculator: Calculator, rates: RateTable, frecency: CurrencyFrecency) {
    var text by rememberSaveable { mutableStateOf("") }
    val result = remember(text, rates) { calculator.calculate(text, rates) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    var ranking by remember { mutableStateOf(emptyList<CurrencyCode>()) }
    LaunchedEffect(frecency) { ranking = frecency.ranking(Instant.now()) }

    // Один набор валют засчитывается один раз, пока ввод не очистят.
    var recorded by remember { mutableStateOf<Set<CurrencyCode>?>(null) }
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

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    visualTransformation = underlineError(result, MaterialTheme.colorScheme.error),
                    decorationBox = { inner ->
                        Column(Modifier.padding(16.dp)) {
                            if (text.isEmpty()) {
                                Text(
                                    stringResource(R.string.input_hint),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Spacer(Modifier.height(24.dp))
            ResultText(result)
            Spacer(Modifier.height(16.dp))
            Conversions(result, ranking, rates, Modifier.weight(1f))
            Text(
                stringResource(R.string.rates_footer, rates.date.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Сумма во всех валютах рейтинга, кроме валюты главной строки; безразмерный результат — без списка (#5). */
@Composable
private fun Conversions(result: Calculation, ranking: List<CurrencyCode>, rates: RateTable, modifier: Modifier) {
    val money = (result as? Calculation.Ok)?.value as? Value.Money
    val rows = if (money == null) emptyList() else ranking.mapNotNull { code ->
        if (code == money.currency) return@mapNotNull null
        val rate = rates.rate(money.currency, code) ?: return@mapNotNull null
        Value.Money(Num(money.amount.value * rate.value), code)
    }
    LazyColumn(modifier.fillMaxWidth()) {
        items(rows, key = { it.currency.code }) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    row.currency.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(format(row.amount.value, 2), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ResultText(result: Calculation) {
    when (result) {
        is Calculation.Ok -> Text(
            formatValue(result.value),
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        is Calculation.Failed -> Text(
            errorText(result.error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Calculation.Incomplete -> Unit
    }
}

/** Подчёркивает span ошибки в тексте. Длина текста не меняется, маппинг тождественный. */
private fun underlineError(result: Calculation, color: androidx.compose.ui.graphics.Color) =
    VisualTransformation { text ->
        val styled = AnnotatedString.Builder(text)
        if (result is Calculation.Failed) {
            val start = result.span.start.coerceIn(0, text.length)
            val end = result.span.end.coerceIn(start, text.length)
            styled.addStyle(SpanStyle(color = color, textDecoration = TextDecoration.Underline), start, end)
        }
        TransformedText(styled.toAnnotatedString(), OffsetMapping.Identity)
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
