package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.analysis.model.TranscriptTurn
import com.santacut.domain.analysis.model.TranscriptTurns
import com.santacut.domain.global.config.ScoreSamplingProperties
import com.santacut.domain.global.config.ScoreShrinkageProperties
import com.santacut.infrastructure.analysis.client.ScoreClient
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 모든 작업자를 합친 채점 호출 자리(`santacut.score.sampling.max-concurrent-calls`)가 동시 호출 수를 묶는지 확인한다.
 * 작업 셋이 한 구간 대화를 네 번씩 동시에 물어도 한꺼번에 나가는 호출은 자리 수를 넘지 않는다.
 */
class AnalysisScoringServiceSlotTest {
    private val scoreClient: ScoreClient = mock(ScoreClient::class.java)
    private val running = AtomicInteger()
    private val peak = AtomicInteger()

    @Test
    fun `여러 작업이 동시에 물어도 한꺼번에 나가는 채점 호출은 앱 전체 자리 수를 넘지 않는다`() {
        val service = AnalysisScoringService(
            scoreClient, JsonMapper.builder().build(), ScoreShrinkageProperties(),
            ScoreSamplingProperties(samples = SAMPLES, maxConcurrentCalls = SLOTS),
        )
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer { slowAnswer() }

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            repeat(JOBS) { executor.submit { service.ask(shortDigest(), SYSTEM_PROMPT) {} } }
        }

        assertEquals(SLOTS, peak.get())
    }

    @Test
    fun `구간 동시 호출 수는 1부터 구간 수 상한까지, 앱 전체 자리는 1 이상만 받는다`() {
        assertFailsWith<IllegalArgumentException> { ScoreSamplingProperties(chunkConcurrency = 0) }
        assertFailsWith<IllegalArgumentException> { ScoreSamplingProperties(chunkConcurrency = 9) }
        assertFailsWith<IllegalArgumentException> { ScoreSamplingProperties(maxConcurrentCalls = 0) }
        assertEquals(6, ScoreSamplingProperties().maxConcurrentCalls)
    }

    private fun slowAnswer(): ScoreAnswer {
        peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
        Thread.sleep(SLOW_CALL_MILLIS)
        running.decrementAndGet()
        return ScoreAnswer.insufficient(ModelUsage(10, 5, "STOP"))
    }

    private fun shortDigest(): ScoringDigest {
        val turns = List(TURNS) { index -> TranscriptTurn(if (index % 2 == 0) ScoringMessage.SELF else PARTNER, NOON, NOON, "가".repeat(LENGTH)) }
        return ScoringDigest(turns.size, turns.size / 2, TranscriptTurns(turns), 0)
    }

    private fun anyChunk(): ScoringChunk = any(ScoringChunk::class.java) ?: ScoringChunk(TranscriptTurns(emptyList()), 1, 0, 0)

    companion object {
        private const val SAMPLES = 4
        private const val SLOTS = 2
        private const val JOBS = 3
        private const val TURNS = 10
        private const val LENGTH = 100
        private const val SLOW_CALL_MILLIS = 100L
        private const val SYSTEM_PROMPT = "채점 프롬프트"
        private const val PARTNER = "상대"
        private val NOON: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)
    }
}
