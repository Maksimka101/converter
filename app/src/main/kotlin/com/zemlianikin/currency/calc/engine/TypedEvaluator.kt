package com.zemlianikin.currency.calc.engine

import com.zemlianikin.currency.calc.Calculation
import com.zemlianikin.currency.calc.CalcError
import com.zemlianikin.currency.calc.Value
import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.Num
import com.zemlianikin.currency.core.RateTable
import java.math.BigDecimal
import java.math.MathContext

// Серый ящик: внутренние типы значения. Процент и отношение наружу не выходят как есть.
private sealed interface Typed {
    /** Сумма в валюте. */
    data class Sum(val amount: BigDecimal, val code: CurrencyCode) : Typed

    /** Обычное число. */
    data class Plain(val value: BigDecimal) : Typed

    /** Процент как доля: 10% хранится как 0.1. */
    data class Pct(val fraction: BigDecimal) : Typed

    /** Валюта ÷ валюта. Пометка живёт, только пока это весь результат. */
    data class Rat(val value: BigDecimal) : Typed

    /** Число без типа: процент и отношение сворачиваются в число. Сумма — не число. */
    val number: BigDecimal
        get() = when (this) {
            is Plain -> value
            is Pct -> fraction
            is Rat -> value
            is Sum -> error("сумма не сворачивается в число")
        }
}

/** Вычисляет дерево по типам #5: проценты, смешанные валюты, отношение. */
class TypedEvaluator : Evaluator {
    override fun eval(node: Node, rates: RateTable): Value =
        when (val v = Run(rates).eval(node)) {
            is Typed.Sum -> Value.Money(Num(v.amount), v.code)
            is Typed.Plain -> Value.Number(Num(v.value))
            is Typed.Pct -> Value.Number(Num(v.fraction))
            is Typed.Rat -> Value.Ratio(Num(v.value))
        }
}

private class Run(private val rates: RateTable) {
    fun eval(node: Node): Typed = when (node) {
        is Node.Number -> Typed.Plain(node.value.value)
        is Node.Currency -> Typed.Sum(BigDecimal.ONE, node.code)
        is Node.WithCurrency -> {
            val v = eval(node.inner)
            if (v is Typed.Sum) fail(CalcError.TwoCurrencies, node)
            Typed.Sum(v.number, node.code)
        }
        is Node.Percent -> {
            val v = eval(node.inner)
            if (v is Typed.Sum) fail(CalcError.MoneyPercent, node)
            Typed.Pct(v.number.movePointLeft(2))
        }
        is Node.Negate -> when (val v = eval(node.inner)) {
            is Typed.Sum -> Typed.Sum(v.amount.negate(), v.code)
            is Typed.Pct -> Typed.Pct(v.fraction.negate())
            else -> Typed.Plain(v.number.negate()) // Ratio тоже становится числом
        }
        is Node.Binary -> binary(node)
        is Node.PercentOf -> {
            val p = eval(node.percent)
            if (p is Typed.Sum) fail(CalcError.MoneyPercent, node.percent)
            scale(eval(node.base), p.number)
        }
        is Node.Convert -> convert(node)
    }

    private fun fail(error: CalcError, node: Node): Nothing =
        throw Stop(Calculation.Failed(error, node.span))

    /** Тип базы сохраняется: сумма остаётся суммой, всё остальное — число. */
    private fun scale(base: Typed, k: BigDecimal): Typed =
        if (base is Typed.Sum) Typed.Sum(base.amount * k, base.code) else Typed.Plain(base.number * k)

    /** Сумма в валюте [to]; та же валюта курса не требует. */
    private fun amountIn(s: Typed.Sum, to: CurrencyCode, node: Node): BigDecimal =
        if (s.code == to) s.amount
        else s.amount * (rates.rate(s.code, to) ?: fail(CalcError.NoRate, node)).value

    private fun quotient(a: BigDecimal, b: BigDecimal) = a.divide(b, MathContext.DECIMAL128)

    private fun binary(node: Node.Binary): Typed {
        // Отношение внутри арифметики — обычное число.
        val l = eval(node.left).let { if (it is Typed.Rat) Typed.Plain(it.value) else it }
        val r = eval(node.right).let { if (it is Typed.Rat) Typed.Plain(it.value) else it }
        return when (node.operation) {
            Operation.Plus -> addSub(node, l, r, minus = false)
            Operation.Minus -> addSub(node, l, r, minus = true)
            Operation.Multiply -> multiply(node, l, r)
            Operation.Divide -> divide(node, l, r)
        }
    }

    private fun addSub(node: Node, l: Typed, r: Typed, minus: Boolean): Typed {
        val sign = if (minus) BigDecimal.ONE.negate() else BigDecimal.ONE
        return when {
            r is Typed.Pct && l is Typed.Pct -> Typed.Pct(l.fraction + sign * r.fraction)
            // Процент справа — от накопленного слева: a × (1 ± p).
            r is Typed.Pct && l is Typed.Sum -> Typed.Sum(l.amount * (BigDecimal.ONE + sign * r.fraction), l.code)
            r is Typed.Pct -> Typed.Plain(l.number * (BigDecimal.ONE + sign * r.fraction))
            l is Typed.Sum && r is Typed.Sum -> Typed.Sum(l.amount + sign * amountIn(r, l.code, node), l.code)
            l is Typed.Sum || r is Typed.Sum -> fail(CalcError.MixedNumberMoney, node)
            else -> Typed.Plain(l.number + sign * r.number)
        }
    }

    private fun multiply(node: Node, l: Typed, r: Typed): Typed = when {
        l is Typed.Sum && r is Typed.Sum -> fail(CalcError.MoneyTimesMoney, node)
        l is Typed.Sum -> Typed.Sum(l.amount * r.number, l.code)
        r is Typed.Sum -> Typed.Sum(l.number * r.amount, r.code)
        l is Typed.Pct && r is Typed.Plain -> Typed.Pct(l.fraction * r.value)
        else -> Typed.Plain(l.number * r.number) // в том числе a * p% = a × p
    }

    private fun divide(node: Node, l: Typed, r: Typed): Typed {
        val divisor = if (r is Typed.Sum) r.amount else r.number
        if (divisor.signum() == 0) fail(CalcError.DivideByZero, node)
        return when {
            l is Typed.Sum && r is Typed.Sum -> Typed.Rat(quotient(l.amount, amountIn(r, l.code, node)))
            l is Typed.Sum -> Typed.Sum(quotient(l.amount, divisor), l.code)
            r is Typed.Sum -> fail(CalcError.DivideByMoney, node)
            else -> Typed.Plain(quotient(l.number, divisor)) // в том числе a / p% = a ÷ p, P ÷ P
        }
    }

    private fun convert(node: Node.Convert): Typed = when (val v = eval(node.inner)) {
        is Typed.Rat -> fail(CalcError.ConvertRatio, node)
        is Typed.Sum -> Typed.Sum(amountIn(v, node.target, node), node.target)
        else -> Typed.Sum(v.number, node.target) // `100 to eur` = 100 eur
    }
}
