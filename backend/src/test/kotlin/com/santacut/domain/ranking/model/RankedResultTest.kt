package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.DataSource
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class RankedResultTest {
    private val computedAt = Instant.parse("2026-12-01T00:00:00Z")
    private val previous = assessment("previous", 7000)
    private val latest = assessment("latest", 9000)

    @Test
    fun `스냅숏이 최신 평가 것이면 둘을 묶고 반영 완료다`() {
        val snapshot = snapshot(latest.id)

        val result = RankedResult.of(latest, snapshot) { fail("최신 평가를 다시 찾지 않는다") }

        assertSame(snapshot, result.snapshot)
        assertSame(latest, result.assessment)
        assertFalse(result.rankingPending)
    }

    @Test
    fun `스냅숏이 이전 평가 것이면 그 평가의 점수와 묶고 반영 대기다`() {
        val snapshot = snapshot(previous.id)

        val result = RankedResult.of(latest, snapshot) { id -> if (id == previous.id) previous else null }

        assertSame(snapshot, result.snapshot)
        assertEquals(7000, result.assessment.scoreHundredths)
        assertTrue(result.rankingPending)
    }

    @Test
    fun `스냅숏이 없으면 순위 없이 최신 평가만 보여 주고 반영 대기다`() {
        val result = RankedResult.of(latest, null) { fail("스냅숏이 없으면 평가를 찾지 않는다") }

        assertNull(result.snapshot)
        assertSame(latest, result.assessment)
        assertTrue(result.rankingPending)
    }

    @Test
    fun `스냅숏의 평가를 찾을 수 없으면 다른 평가의 순위를 섞지 않고 최신 평가만 보여 준다`() {
        val result = RankedResult.of(latest, snapshot("deleted")) { null }

        assertNull(result.snapshot)
        assertSame(latest, result.assessment)
        assertTrue(result.rankingPending)
    }

    private fun snapshot(assessmentId: String): ResultSnapshot =
        ResultSnapshot("snapshot-$assessmentId", "user", assessmentId, 3, 3, computedAt, computedAt)

    private fun assessment(id: String, score: Int): Assessment =
        Assessment(id, "user", DataSource.KAKAO, score, "판정", "한마디", "[]", computedAt)
}
