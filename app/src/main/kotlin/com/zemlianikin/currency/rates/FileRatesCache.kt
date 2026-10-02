package com.zemlianikin.currency.rates

import com.zemlianikin.currency.core.CurrencyCode
import com.zemlianikin.currency.core.Decimal
import com.zemlianikin.currency.core.Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Instant

/**
 * Офлайн-кэш курсов в одном текстовом файле: один снимок — один файл.
 * Заменить реализацию можно через [RatesCache].
 *
 * Формат:
 * ```
 * v1
 * <fetchedAt в epoch millis>
 * <дата курсов, ISO>
 * КОД значение
 * ...
 * ```
 * Значение пишется через `Decimal.toString()` (для очень малых чисел это научная запись, точность не теряется).
 *
 * Запись атомарная: во временный файл рядом и переименование. Чтение любой битой версии даёт null,
 * файл при этом не трогаем.
 */
class FileRatesCache(private val file: File) : RatesCache {

    override suspend fun load(): CachedRates? = withContext(Dispatchers.IO) {
        try {
            if (file.isFile) parse(file.readLines(StandardCharsets.UTF_8)) else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun save(rates: CachedRates) {
        withContext(Dispatchers.IO) {
            val text = format(rates)
            val dir = file.absoluteFile.parentFile
            dir?.mkdirs()
            val tmp = File(dir, file.name + ".tmp")
            try {
                tmp.writeText(text, StandardCharsets.UTF_8)
                Files.move(
                    tmp.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } finally {
                tmp.delete()
            }
        }
    }

    private fun format(rates: CachedRates): String = buildString {
        append(VERSION).append('\n')
        append(rates.fetchedAt.toEpochMilliseconds()).append('\n')
        append(rates.snapshot.date).append('\n')
        for ((code, num) in rates.snapshot.perBase) {
            append(code.code).append(' ').append(num.value.toString()).append('\n')
        }
    }

    /** Строгий разбор: любое отклонение от формата → null. */
    private fun parse(lines: List<String>): CachedRates? {
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

    private companion object {
        const val VERSION = "v1"
    }
}
