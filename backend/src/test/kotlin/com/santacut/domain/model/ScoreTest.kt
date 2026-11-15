package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScoreTest {
    @Test
    fun `항목 점수에 30 25 25 20 가중치를 곱해 100분의 1점 단위 합계를 낸다`() {
        assertEquals(7600, Score.weighted(scores("80", "90", "70", "60")).hundredths)
        assertEquals(10000, Score.weighted(scores("100", "100", "100", "100")).hundredths)
        assertEquals(0, Score.weighted(scores("0", "0", "0", "0")).hundredths)
    }

    @Test
    fun `항목 가중치 합은 100퍼센트라서 가중 합계가 100분의 1점 단위가 된다`() {
        assertEquals(ScoreDimension.TOTAL_WEIGHT, ScoreDimension.entries.sumOf { it.weight })
    }

    @Test
    fun `소수점 둘째 자리 점수도 합계에 그대로 반영한다`() {
        assertEquals(8245, Score.weighted(scores("82.45", "82.45", "82.45", "82.45")).hundredths)
    }

    @Test
    fun `항목 점수가 0~100 범위를 벗어나면 점수 범위 오류다`() {
        assertScoreRejected(scores("100.01", "0", "0", "0"))
        assertScoreRejected(scores("-1", "0", "0", "0"))
    }

    @Test
    fun `소수점 셋째 자리 이하가 있으면 점수 범위 오류다`() {
        assertScoreRejected(scores("10.005", "0", "0", "0"))
    }

    @Test
    fun `합계 점수로 재미용 판정을 고른다`() {
        assertEquals(ScoreVerdict.GIFT_DEPARTED, Score.weighted(scores("80", "90", "70", "60")).verdict())
    }

    private fun assertScoreRejected(components: List<BigDecimal>) {
        val exception = assertFailsWith<SantaCutException> { Score.weighted(components) }
        assertEquals(ErrorCode.AI_SCORE_OUT_OF_RANGE, exception.errorCode)
    }

    private fun scores(vararg values: String): List<BigDecimal> = values.map(::BigDecimal)
}
