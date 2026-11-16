package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AssessmentTest {
    private val recordedAt = Instant.parse("2026-11-16T00:00:00Z")
    private val participantId = UUID.randomUUID()
    private val job = AnalysisJob.receive(
        participantId, IdempotencyKey.from("key-0123456789abcdef"), DataSource.KAKAO, "hash", recordedAt,
    )

    @Test
    fun `평가를 기록하면 점수, 판정, 쓴 채점 프롬프트 판 번호를 함께 남긴다`() {
        val score = Score.shrunk(listOf(BigDecimal("80"), BigDecimal("90"), BigDecimal("70"), BigDecimal("60")), LowConfidenceDimensions.NONE, null)

        val assessment = Assessment.record(job, ScoredAssessment(score, PROMPT_VERSION, QUOTE, EarnedBadges(emptyList())), COMPONENTS_JSON, recordedAt)

        assertEquals(participantId.toString(), assessment.userId)
        assertEquals(DataSource.KAKAO, assessment.source)
        assertEquals(score.hundredths, assessment.scoreHundredths)
        assertEquals(score.verdict().title, assessment.verdict)
        assertEquals(score.verdict().comment, assessment.comment)
        assertEquals(COMPONENTS_JSON, assessment.components)
        assertEquals(recordedAt, assessment.createdAt)
        assertEquals(Score.RULE_VERSION, assessment.scoreRuleVersion)
        assertEquals(QUOTE, assessment.quote)
        assertEquals(PROMPT_VERSION, assessment.promptVersion)
    }

    @Test
    fun `같은 채점 입력의 지난 평가를 새 ID와 기록 시각으로 옮겨 적고 점수, 판정, 판 번호는 그대로 둔다`() {
        val previous = Assessment("assessment-previous", "user", DataSource.KAKAO, 7000, "판정", "한마디", "[]", recordedAt, PROMPT_VERSION)
        val reusedAt = recordedAt.plusSeconds(60)

        val reused = Assessment.reuse(job, previous, reusedAt)

        assertNotEquals(previous.id, reused.id)
        assertEquals(participantId.toString(), reused.userId)
        assertEquals(reusedAt, reused.createdAt)
        assertEquals(
            listOf(previous.scoreHundredths, previous.verdict, previous.comment, previous.components, previous.promptVersion),
            listOf(reused.scoreHundredths, reused.verdict, reused.comment, reused.components, reused.promptVersion),
        )
    }

    @Test
    fun `프롬프트를 DB로 옮기기 전 평가는 판 번호가 없다`() {
        val legacy = Assessment("assessment-legacy", "user", DataSource.KAKAO, 7000, "판정", "한마디", "[]", recordedAt)

        assertNull(legacy.promptVersion)
    }

    companion object {
        private const val QUOTE = "덕분에 살았다"
        private const val PROMPT_VERSION = 3
        private const val COMPONENTS_JSON = "[]"
    }
}
