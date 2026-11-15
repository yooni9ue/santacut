package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoreVerdictsTest {
    @Test
    fun `높은 구간부터 점수가 하한 이상인 첫 판정을 찾는다`() {
        val verdicts = ScoreVerdicts(listOf(ScoreVerdict.SANTA_FAMILY, ScoreVerdict.GIFT_DEPARTED))
        assertEquals(ScoreVerdict.SANTA_FAMILY, verdicts.find(9000))
        assertEquals(ScoreVerdict.GIFT_DEPARTED, verdicts.find(8999))
        assertNull(verdicts.find(7499))
    }
}
