package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class ArchetypeTest {
    @Test
    fun `가장 높은 항목과 장난기 높낮이로 8가지 유형을 고른다`() {
        assertEquals(Archetype.HOT_COCOA_COURIER, Archetype.of(components(90, 70, 70, 70), 70, SPLIT))
        assertEquals(Archetype.FIREPLACE_KEEPER, Archetype.of(components(90, 70, 70, 70), 50, SPLIT))
        assertEquals(Archetype.CAROL_LEADER, Archetype.of(components(60, 90, 70, 70), 90, SPLIT))
        assertEquals(Archetype.TREE_TOP_STAR, Archetype.of(components(60, 90, 70, 70), 10, SPLIT))
        assertEquals(Archetype.MANNER_SNOWMAN, Archetype.of(components(60, 70, 90, 70), 70, SPLIT))
        assertEquals(Archetype.GIFT_WRAPPER, Archetype.of(components(60, 70, 90, 70), 30, SPLIT))
        assertEquals(Archetype.RUDOLPH_DRIVER, Archetype.of(components(60, 70, 70, 90), 70, SPLIT))
        assertEquals(Archetype.WORKSHOP_ELF, Archetype.of(components(60, 70, 70, 90), 30, SPLIT))
    }

    @Test
    fun `위 두 항목 차이가 5점보다 작으면 평균에 따라 균형형이다`() {
        assertEquals(Archetype.SANTA_CANDIDATE, Archetype.of(components(80, 77, 60, 60), 90, SPLIT))
        assertEquals(Archetype.GRINCH_APPRENTICE, Archetype.of(components(40, 38, 30, 30), 90, SPLIT))
    }

    @Test
    fun `장난기를 모르는 예전 평가는 장난기 낮음으로 본다`() {
        assertEquals(Archetype.FIREPLACE_KEEPER, Archetype.of(components(90, 70, 70, 70), null, SPLIT))
    }

    @Test
    fun `가장 약한 항목은 점수가 가장 낮은 항목이다`() {
        assertEquals(ScoreDimension.POSITIVITY_SUPPORT, components(90, 30, 70, 70).rankedDimensions().last())
    }

    @Test
    fun `평가 행의 항목 점수 JSON을 읽는다`() {
        assertEquals(BigDecimal("63.25"), ScoreComponents.fromColumn("[63.25,71,55,66]").scoreOf(ScoreDimension.CARE_EMPATHY))
    }

    private fun components(vararg values: Int): ScoreComponents = ScoreComponents(values.map { BigDecimal(it) })

    companion object {
        private const val SPLIT = 50
    }
}
