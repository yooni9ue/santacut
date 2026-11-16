package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class ScoreShrinkageTest {
    private val shrinkage = ScoreShrinkage(BigDecimal.ONE, 20, personalPrior = false)

    @Test
    fun `기회가 여러 번이고 본인 턴이 충분하면 근거 무게 2로 사전 평균 쪽으로 3분의 1만 당긴다`() {
        assertEquals(0, BigDecimal(75).compareTo(shrinkage.shrink(BigDecimal(85), 2, 40)))
    }

    @Test
    fun `기회가 없으면 사전 평균 그대로다`() {
        assertEquals(0, BigDecimal(55).compareTo(shrinkage.shrink(BigDecimal(90), 0, 40)))
    }

    @Test
    fun `본인 턴이 기준의 절반이면 근거 무게도 절반이다`() {
        assertEquals(0, BigDecimal(70).compareTo(shrinkage.shrink(BigDecimal(85), 2, 10)))
    }

    @Test
    fun `세기 0은 당기지 않는다`() {
        assertEquals(BigDecimal(90), ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false).shrink(BigDecimal(90), 0, 0))
    }
}
