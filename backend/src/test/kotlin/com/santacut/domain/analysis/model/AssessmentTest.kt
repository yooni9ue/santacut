package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssessmentTest {
    private val recordedAt = Instant.parse("2026-11-16T00:00:00Z")
    private val participantId = UUID.randomUUID()
    private val job = AnalysisJob.receive(
        participantId, IdempotencyKey.from("key-0123456789abcdef"), DataSource.KAKAO, "hash", recordedAt,
    )

    @Test
    fun `평가를 기록하면 점수, 판정, 쓴 채점 프롬프트 판 번호를 함께 남긴다`() {
        val score = Score.weighted(listOf(BigDecimal("80"), BigDecimal("90"), BigDecimal("70"), BigDecimal("60")))

        val assessment = Assessment.record(job, score, COMPONENTS_JSON, PROMPT_VERSION, recordedAt)

        assertEquals(participantId.toString(), assessment.userId)
        assertEquals(DataSource.KAKAO, assessment.source)
        assertEquals(score.hundredths, assessment.scoreHundredths)
        assertEquals(score.verdict().title, assessment.verdict)
        assertEquals(score.verdict().comment, assessment.comment)
        assertEquals(COMPONENTS_JSON, assessment.components)
        assertEquals(recordedAt, assessment.createdAt)
        assertEquals(PROMPT_VERSION, assessment.promptVersion)
    }

    @Test
    fun `프롬프트를 DB로 옮기기 전 평가는 판 번호가 없다`() {
        val legacy = Assessment("assessment-legacy", "user", DataSource.KAKAO, 7000, "판정", "한마디", "[]", recordedAt)

        assertNull(legacy.promptVersion)
    }

    companion object {
        private const val PROMPT_VERSION = 3
        private const val COMPONENTS_JSON = "[]"
    }
}
