package com.santacut.application.ranking.runner

import com.santacut.MutableClock
import com.santacut.domain.ranking.service.RankingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 기다리기([RankingRebuildDebounceRunner]의 pause)를 바꿔 끼워 시간에 기대지 않고 확인한다.
 * 기다림은 [gate]가 열릴 때까지 막히고, 기다린 시간은 [waits]에 남는다.
 */
class RankingRebuildDebounceRunnerTest {
    private val rankingService: RankingService = mock(RankingService::class.java)
    private val clock = MutableClock(START)
    private val waits = LinkedBlockingQueue<Duration>()
    private var gate = CountDownLatch(0)
    private val runner = RankingRebuildDebounceRunner(rankingService, clock) { delay ->
        waits.add(delay)
        gate.await(WAIT_SECONDS, TimeUnit.SECONDS)
    }

    @BeforeEach
    fun setUp() {
        given(rankingService.rebuildDebounce()).willReturn(DEBOUNCE)
        given(rankingService.rebuildMinInterval()).willReturn(MIN_INTERVAL)
    }

    @Test
    fun `기다리는 동안 여러 스레드에서 들어온 요청은 한 번의 계산으로 모인다`() {
        gate = CountDownLatch(1)
        val rebuilds = AtomicInteger()
        val done = CountDownLatch(1)

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            repeat(REQUESTS) { executor.submit { runner.rebuildSoon { rebuild(rebuilds, done) } } }
        }
        gate.countDown()

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS))
        assertEquals(1, rebuilds.get())
        assertEquals(1, waits.size)
    }

    @Test
    fun `계산이 잠금을 잡은 뒤 들어온 요청은 다음 계산을 한 번 더 예약한다`() {
        val rebuilds = AtomicInteger()
        val first = CountDownLatch(1)
        val second = CountDownLatch(2)

        runner.rebuildSoon { rebuild(rebuilds, first, second) }
        assertTrue(first.await(WAIT_SECONDS, TimeUnit.SECONDS))
        runner.rebuildSoon { rebuild(rebuilds, second) }

        assertTrue(second.await(WAIT_SECONDS, TimeUnit.SECONDS))
        assertEquals(2, rebuilds.get())
    }

    @Test
    fun `지난 계산이 끝난 지 최소 간격이 안 됐으면 남은 시간만큼, 지났으면 모으기 시간만큼 기다린다`() {
        assertEquals(DEBOUNCE, scheduleAndRebuild())

        clock.moveTo(START.plusSeconds(2))
        assertEquals(Duration.ofSeconds(3), scheduleAndRebuild())

        clock.moveTo(START.plusSeconds(60))
        assertEquals(DEBOUNCE, scheduleAndRebuild())
    }

    @Test
    fun `계산이 잠금까지 가지 못하고 예외를 던져도 다음 요청은 다시 예약된다`() {
        val failed = CountDownLatch(1)
        val next = CountDownLatch(1)

        runner.rebuildSoon {
            failed.countDown()
            throw IllegalStateException("계산 실패")
        }
        assertTrue(failed.await(WAIT_SECONDS, TimeUnit.SECONDS))

        assertTrue(waitUntil { runner.rebuildSoon { next.countDown() }.let { next.count == 0L } })
    }

    @Test
    fun `앱이 꺼지는 중이면 기다리던 계산을 건너뛰고 새 요청도 받지 않는다`() {
        gate = CountDownLatch(1)
        val rebuilds = AtomicInteger()

        runner.rebuildSoon { rebuilds.incrementAndGet() }
        assertTrue(waitUntil { waits.size == 1 })
        runner.close()
        gate.countDown()
        runner.rebuildSoon { rebuilds.incrementAndGet() }

        assertTrue(waitUntil { waits.size == 1 })
        assertEquals(0, rebuilds.get())
    }

    @Test
    fun `커밋 직후 다시 계산을 꺼 두었으면 예약하지 않는다`() {
        given(rankingService.rebuildDebounce()).willReturn(null)
        val rebuilds = AtomicInteger()

        runner.rebuildSoon { rebuilds.incrementAndGet() }

        assertTrue(waits.isEmpty())
        assertEquals(0, rebuilds.get())
    }

    /** 계산하는 쪽(facade)처럼 잠금을 잡은 직후 예약 표시를 내리고, 끝나면 끝난 시각을 남긴다. */
    private fun rebuild(rebuilds: AtomicInteger, vararg done: CountDownLatch) {
        runner.rebuildStarted()
        rebuilds.incrementAndGet()
        runner.rebuildFinished()
        done.forEach(CountDownLatch::countDown)
    }

    /** 요청 하나를 예약해 계산이 끝날 때까지 기다리고, 그 예약이 기다린 시간을 돌려준다. */
    private fun scheduleAndRebuild(): Duration {
        val done = CountDownLatch(1)
        runner.rebuildSoon { rebuild(AtomicInteger(), done) }
        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS))
        return waits.poll(WAIT_SECONDS, TimeUnit.SECONDS) ?: Duration.ZERO
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS)
        while (!condition()) {
            if (System.nanoTime() > deadline) return false
            Thread.sleep(POLL_MILLIS)
        }
        return true
    }

    companion object {
        private const val REQUESTS = 20
        private const val WAIT_SECONDS = 5L
        private const val POLL_MILLIS = 5L
        private val START: Instant = Instant.parse("2026-11-20T00:00:00Z")
        private val DEBOUNCE: Duration = Duration.ofSeconds(1)
        private val MIN_INTERVAL: Duration = Duration.ofSeconds(5)
    }
}
