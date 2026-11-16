package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreAnswerCallsTest {
    @Test
    fun `여러 호출은 동시에 불리고 모두 모인다`() {
        val started = CountDownLatch(COUNT)
        val answers = ScoreAnswerCalls.concurrently(COUNT) {
            started.countDown()
            assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "다른 호출과 동시에 불리지 않았다")
            ScoreAnswer.insufficient(USAGE)
        }

        assertEquals(COUNT, answers.size)
    }

    @Test
    fun `한 호출이 예외를 던지면 나머지 호출이 끝난 뒤 그 호출을 버릴 답으로 바꾸고 예외는 합칠 때 그대로 던진다`() {
        val finished = AtomicInteger()
        val calls = AtomicInteger()

        val answers = ScoreAnswerCalls.concurrently(COUNT) {
            if (calls.getAndIncrement() == 0) throw IllegalStateException("AI 장애")
            Thread.sleep(SLOW_CALL_MILLIS)
            ScoreAnswer.insufficient(USAGE).also { finished.incrementAndGet() }
        }

        assertEquals(COUNT - 1, finished.get())
        assertEquals(COUNT - 1, answers.count { answer -> answer.isUsable() })
        val exception = assertThrows<IllegalStateException> { answers.single { answer -> !answer.isUsable() }.verifyAnswered() }
        assertEquals("AI 장애", exception.message)
    }

    @Test
    fun `실행 예외가 아닌 오류는 그대로 던진다`() {
        val calls = AtomicInteger()

        assertThrows<AssertionError> {
            ScoreAnswerCalls.concurrently(COUNT) {
                if (calls.getAndIncrement() == 0) throw AssertionError("멈춤")
                ScoreAnswer.insufficient(USAGE)
            }
        }
    }

    companion object {
        private const val COUNT = 3
        private const val TIMEOUT_SECONDS = 5L
        private const val SLOW_CALL_MILLIS = 100L
        private val USAGE = ModelUsage(10, 5, "STOP")
    }
}
