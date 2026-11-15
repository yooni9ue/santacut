package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreComponentsTest {
    @Test
    fun `항목 점수에 가중치를 곱해 100분의 1점 단위로 합한다`() {
        assertEquals(7600, components("80", "90", "70", "60").weightedHundredths())
        assertEquals(8245, components("82.45", "82.45", "82.45", "82.45").weightedHundredths())
    }

    @Test
    fun `0~100점이고 소수 둘째 자리까지인 점수만 올바르다`() {
        assertTrue(components("0", "100", "50.5", "99.99").isValid())
        assertFalse(components("-1", "50", "50", "50").isValid())
        assertFalse(components("100.01", "50", "50", "50").isValid())
        assertFalse(components("50.001", "50", "50", "50").isValid())
    }

    @Test
    fun `저장용 목록은 받은 순서를 지킨다`() {
        assertEquals(listOf(BigDecimal("80"), BigDecimal("90")), components("80", "90").toList())
    }

    private fun components(vararg scores: String): ScoreComponents = ScoreComponents(scores.map(::BigDecimal))
}
