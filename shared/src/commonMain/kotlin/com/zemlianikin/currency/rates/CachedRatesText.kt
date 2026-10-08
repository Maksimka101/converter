package com.zemlianikin.currency.rates

import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.Decimal
import com.zemlianikin.currency.core.Num
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

// Текстовая запись снимка курсов для офлайн-кэша: файл на JVM, localStorage в браузере.
//
// Формат:
//   v1
//   <fetchedAt в epoch millis>
//   <дата курсов, ISO>
//   КОД значение
//   ...
// Значение пишется через `Decimal.toString()` (для очень малых чисел это научная запись, точность не теряется).

private const val VERSION = "v1"

internal fun encodeCachedRates(rates: CachedRates): String = buildString {
    append(VERSION).append('\n')
    append(rates.fetchedAt.toEpochMilliseconds()).append('\n')
    append(rates.snapshot.date).append('\n')
    for ((code, num) in rates.snapshot.perBase) {
        append(code.code).append(' ').append(num.value.toString()).append('\n')
    }
}

/** Строгий разбор: любое отклонение от формата → null или исключение. */
internal fun decodeCachedRates(lines: List<String>): CachedRates? {
    if (lines.size < 4 || lines[0] != VERSION) return null
    val fetchedAt = Instant.fromEpochMilliseconds(lines[1].toLong())
    val date = LocalDate.parse(lines[2])
    val perBase = LinkedHashMap<CurrencyCode, Num>()
    for (line in lines.drop(3)) {
        val parts = line.split(' ')
        if (parts.size != 2 || parts[0].isEmpty()) return null
        perBase[CurrencyCode(parts[0])] = Num(Decimal(parts[1]))
    }
    if (perBase.isEmpty()) return null
    return CachedRates(RatesSnapshot(date, perBase), fetchedAt)
}
