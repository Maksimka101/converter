package com.zemlianikin.currency.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Стор в памяти не приостанавливается, поэтому корутина завершается синхронно — без kotlinx.coroutines. */
private fun <T> runBlocking(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

class DecayingFrecencyTest {
    private class MemoryStore : CurrencyUsageStore {
        val data = mutableMapOf<CurrencyCode, CurrencyUsage>()
        override suspend fun load() = data.toMap()
        override suspend fun save(changes: Map<CurrencyCode, CurrencyUsage>) { data += changes }
    }

    private fun codes(vararg c: String) = c.map(::CurrencyCode)
    private val t0 = Instant.parse("2026-09-25T12:00:00Z")
    private val known = codes("USD", "EUR", "RUB", "JPY")

    private fun frecency(seed: List<CurrencyCode> = codes("USD", "EUR"), store: CurrencyUsageStore = MemoryStore()) =
        DecayingFrecency(store, known, seed)

    @Test fun `cold start ranks seed first and then the rest`() = runBlocking {
        assertEquals(codes("USD", "EUR", "RUB", "JPY"), frecency().ranking(t0))
    }

    @Test fun `seed outside known list is ignored`() = runBlocking {
        assertEquals(codes("EUR", "USD", "RUB", "JPY"), frecency(seed = codes("EUR", "CHF", "USD")).ranking(t0))
    }

    @Test fun `one use equals seed weight and second use overtakes it`() = runBlocking {
        val f = frecency()
        f.recordUsed(setOf(CurrencyCode("RUB")), t0)
        assertEquals(codes("USD", "EUR", "RUB", "JPY"), f.ranking(t0))
        f.recordUsed(setOf(CurrencyCode("RUB")), t0)
        assertEquals(codes("RUB", "USD", "EUR", "JPY"), f.ranking(t0))
    }

    @Test fun `score halves after half life`() = runBlocking {
        val store = MemoryStore()
        val f = frecency(store = store)
        val later = t0.plus(Duration.ofDays(21))
        repeat(2) { f.recordUsed(setOf(CurrencyCode("RUB")), t0) } // RUB = 2.0 → 1.0 через полураспад
        repeat(3) { f.recordUsed(setOf(CurrencyCode("JPY")), later) } // JPY = 3.0, seed USD/EUR = 0.5
        assertEquals(codes("JPY", "RUB", "USD", "EUR"), f.ranking(later))
        assertEquals(3.0, store.data.getValue(CurrencyCode("JPY")).score, 1e-9)
    }

    @Test fun `repeated use accumulates`() = runBlocking {
        val store = MemoryStore()
        val f = frecency(store = store)
        repeat(3) { f.recordUsed(setOf(CurrencyCode("JPY")), t0) }
        assertEquals(3.0, store.data.getValue(CurrencyCode("JPY")).score, 1e-9)
        assertEquals(CurrencyCode("JPY"), f.ranking(t0).first())
    }

    @Test fun `locale seed puts country currency first`() {
        assertEquals(codes("RUB", "USD", "EUR", "GBP", "CNY", "JPY"), localeSeed(Locale.forLanguageTag("ru-RU")))
        assertEquals(codes("USD", "EUR", "GBP", "CNY", "JPY"), localeSeed(Locale.US))
        assertEquals(codes("USD", "EUR", "GBP", "CNY", "JPY"), localeSeed(Locale.ENGLISH))
    }
}
