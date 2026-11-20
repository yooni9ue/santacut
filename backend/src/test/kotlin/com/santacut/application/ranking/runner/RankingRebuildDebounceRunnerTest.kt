package com.santacut.application.ranking.runner

import com.santacut.domain.ranking.service.RankingService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RankingRebuildDebounceRunnerTest {
    private val rankingService: RankingService = mock(RankingService::class.java)
    private val runner = RankingRebuildDebounceRunner(rankingService)

    @Test
    fun `기다리는 동안 여러 스레드에서 들어온 요청은 한 번의 계산으로 모인다`() {
        given(rankingService.rebuildDebounce()).willReturn(DEBOUNCE)
        val rebuilds = AtomicInteger()
        val done = CountDownLatch(1)

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            repeat(REQUESTS) { executor.submit { runner.rebuildSoon { rebuilds.incrementAndGet().also { done.countDown() } } } }
        }

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS))
        Thread.sleep(DEBOUNCE.toMillis() * 2)
        assertEquals(1, rebuilds.get())
    }

    @Test
    fun `계산이 시작된 뒤 들어온 요청은 다음 계산을 한 번 더 예약한다`() {
        given(rankingService.rebuildDebounce()).willReturn(DEBOUNCE)
        val rebuilds = AtomicInteger()
        val first = CountDownLatch(1)
        val second = CountDownLatch(2)
        val record: () -> Unit = {
            rebuilds.incrementAndGet()
            first.countDown()
            second.countDown()
        }

        runner.rebuildSoon(record)
        assertTrue(first.await(WAIT_SECONDS, TimeUnit.SECONDS))
        runner.rebuildSoon(record)

        assertTrue(second.await(WAIT_SECONDS, TimeUnit.SECONDS))
        assertEquals(2, rebuilds.get())
    }

    @Test
    fun `계산이 예외를 던져도 다음 요청은 다시 예약된다`() {
        given(rankingService.rebuildDebounce()).willReturn(DEBOUNCE)
        val attempts = CountDownLatch(2)

        runner.rebuildSoon { attempts.countDown(); throw IllegalStateException("계산 실패") }
        Thread.sleep(DEBOUNCE.toMillis() * 3)
        runner.rebuildSoon { attempts.countDown() }

        assertTrue(attempts.await(WAIT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `커밋 직후 다시 계산을 꺼 두었으면 예약하지 않는다`() {
        given(rankingService.rebuildDebounce()).willReturn(null)
        val rebuilds = AtomicInteger()

        runner.rebuildSoon { rebuilds.incrementAndGet() }
        Thread.sleep(DEBOUNCE.toMillis() * 2)

        assertEquals(0, rebuilds.get())
    }

    companion object {
        private const val REQUESTS = 20
        private const val WAIT_SECONDS = 5L
        private val DEBOUNCE: Duration = Duration.ofMillis(100)
    }
}
