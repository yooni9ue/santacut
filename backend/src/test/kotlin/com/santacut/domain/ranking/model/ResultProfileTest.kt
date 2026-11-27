package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.Assessments
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoreDimension
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResultProfileTest {
    @Test
    fun `참가자가 100명 이상일 때만 상위 비율을 올림으로 낸다`() {
        val mine = assessment("me", "[90,70,70,70]", 70)
        val population = Assessments(listOf(mine))

        assertNull(ResultProfile.of(mine, snapshot(3), ResultPopulation.of(population), 99).topPercent)
        assertEquals(3, ResultProfile.of(mine, snapshot(3), ResultPopulation.of(population), 100).topPercent)
        assertEquals(1, ResultProfile.of(mine, snapshot(1), ResultPopulation.of(population), 1000).topPercent)
    }

    @Test
    fun `회귀 - 참가자가 셋이면 상위 비율은 비우고 참가자 수는 남긴다`() {
        val mine = assessment("me", "[90,70,70,70]", 70)
        val profile = ResultProfile.of(mine, snapshot(2), ResultPopulation.of(Assessments(listOf(mine))), 3)

        assertNull(profile.topPercent)
        assertEquals(3, profile.participantCount)
    }

    @Test
    fun `순위가 남은 참가자 수보다 커도 상위 비율은 100을 넘지 않는다`() {
        val mine = assessment("me", "[90,70,70,70]", 70)

        assertEquals(100, ResultProfile.of(mine, snapshot(500), ResultPopulation.of(Assessments(listOf(mine))), 100).topPercent)
    }

    @Test
    fun `장난기를 아는 참가자가 30명보다 적으면 기준값 50, 이상이면 중앙값으로 유형을 가른다`() {
        val mine = assessment("me", "[90,70,70,70]", 60)
        val few = ResultPopulation.of(Assessments(listOf(mine)))
        val many = ResultPopulation.of(Assessments(List(30) { assessment("u$it", "[60,60,60,90]", 70) } + mine))

        assertEquals(Archetype.HOT_COCOA_COURIER, ResultProfile.of(mine, null, few, 0).archetype)
        assertEquals(Archetype.FIREPLACE_KEEPER, ResultProfile.of(mine, null, many, 0).archetype)
        assertEquals(3, ResultProfile.of(mine, null, many, 0).archetypeSharePercent)
        assertNull(ResultProfile.of(mine, null, few, 0).archetypeSharePercent)
    }

    @Test
    fun `가장 약한 항목은 점수가 가장 낮은 항목이다`() {
        val mine = assessment("me", "[90,70,40,70]", 50)

        assertEquals(ScoreDimension.RESPECTFUL_COMMUNICATION, ResultProfile.of(mine, null, ResultPopulation.of(Assessments(listOf(mine))), 0).weakestDimension())
    }

    private fun assessment(id: String, components: String, playfulness: Int): Assessment =
        Assessment(id, id, DataSource.KAKAO, 7000, "판정", "한마디", components, AT, 6, null, playfulness, 2, null, null)

    private fun snapshot(rank: Int): ResultSnapshot = ResultSnapshot("s", "me", "me", rank, rank, AT, AT)

    companion object {
        private val AT: Instant = Instant.parse("2026-11-01T00:00:00Z")
    }
}
