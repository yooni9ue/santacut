package com.santacut.domain.ranking.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RankedAssessmentTimesTest {
    @Test
    fun `참가자별 순위 반영 시각을 찾고 순위에 오른 적 없는 참가자는 null이다`() {
        val assessedAt = Instant.parse("2026-12-01T00:00:00Z")
        val times = RankedAssessmentTimes(listOf(RankedAssessmentTime("ranked", assessedAt)))

        assertEquals(assessedAt, times.of("ranked"))
        assertNull(times.of("unranked"))
    }
}
