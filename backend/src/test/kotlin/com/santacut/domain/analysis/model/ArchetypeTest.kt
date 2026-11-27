package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArchetypeTest {
    @Test
    fun `유형 이름은 단톡 말투 이름이고 코드는 그대로다`() {
        val names = Archetype.entries.associate { archetype -> archetype.name to archetype.koreanName }

        assertEquals(
            mapOf(
                "HOT_COCOA_COURIER" to "헐 먼저 치는 애", "FIREPLACE_KEEPER" to "읽씹 안 하는 애",
                "CAROL_LEADER" to "호들갑 원탑", "TREE_TOP_STAR" to "하트 먼저 누르는 애",
                "MANNER_SNOWMAN" to "웃긴데 무해한 애", "GIFT_WRAPPER" to "선 안 넘는 애",
                "RUDOLPH_DRIVER" to "갑자기 약속 잡는 애", "WORKSHOP_ELF" to "총대 메는 애",
                "SANTA_CANDIDATE" to "육각형 인간", "GRINCH_APPRENTICE" to "ㅇㅇ만 치는 애",
            ),
            names,
        )
    }

    @Test
    fun `석탄 덱은 같은 항목의 차분한 이름을 싣는다`() {
        assertEquals("읽씹 안 하는 애", Archetype.HOT_COCOA_COURIER.forCoal().koreanName)
        assertEquals("하트 먼저 누르는 애", Archetype.CAROL_LEADER.forCoal().koreanName)
        assertEquals("선 안 넘는 애", Archetype.MANNER_SNOWMAN.forCoal().koreanName)
        assertEquals("총대 메는 애", Archetype.RUDOLPH_DRIVER.forCoal().koreanName)
        assertEquals("ㅇㅇ만 치는 애", Archetype.SANTA_CANDIDATE.forCoal().koreanName)
        assertEquals(Archetype.FIREPLACE_KEEPER, Archetype.FIREPLACE_KEEPER.forCoal())
    }
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
    fun `위 두 항목 차이가 3점보다 작으면 평균에 따라 균형형이다`() {
        assertEquals(Archetype.SANTA_CANDIDATE, Archetype.of(components(80, 78, 60, 60), 90, SPLIT))
        assertEquals(Archetype.GRINCH_APPRENTICE, Archetype.of(components(40, 38, 30, 30), 90, SPLIT))
    }

    @Test
    fun `위 두 항목 차이가 3점 이상 7점 미만이면 수축 전 평균의 차이가 6점 미만일 때 균형형이고 아니면 그 순서로 가른다`() {
        val shrunk = components(84, 80, 60, 60)

        assertEquals(Archetype.SANTA_CANDIDATE, Archetype.of(shrunk, 10, SPLIT, components(90, 88, 60, 60)))
        assertEquals(Archetype.SANTA_CANDIDATE, Archetype.of(shrunk, 10, SPLIT, components(90, 85, 60, 60)))
        assertEquals(Archetype.TREE_TOP_STAR, Archetype.of(shrunk, 10, SPLIT, components(80, 90, 60, 60)))
        assertEquals(Archetype.SANTA_CANDIDATE, Archetype.of(shrunk, 10, SPLIT))
    }

    @Test
    fun `위 두 항목 차이가 5점 경계 근처에서 흔들려도 수축 전 평균이 같으면 유형이 바뀌지 않는다`() {
        // 실제로 같은 파일이 78점대와 80점대로 갈려 트리 꼭대기 별과 산타 후보생으로 나뉜 경우를 본뜬다.
        // 응원이 가장 높고 공감과의 차이가 5점 근처라, 예전 규칙(5점 미만 균형형)이면 흔들림만으로 유형이 바뀐다.
        val raw = components(84, 92, 74, 70)
        val archetypes = LEVEL_NOISE.flatMap { level ->
            GAP_NOISE.map { care ->
                val shrunk = listOf(80 + care + level, 85 + level, 72 + level, 70 + level).map { BigDecimal(it) }
                Archetype.of(ScoreComponents(shrunk), 10, SPLIT, raw)
            }
        }.toSet()

        assertEquals(setOf(Archetype.TREE_TOP_STAR), archetypes)
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
    fun `상수 이름으로 유형을 읽고 모르는 이름은 null이다`() {
        assertEquals(Archetype.WORKSHOP_ELF, Archetype.ofName("WORKSHOP_ELF"))
        assertNull(Archetype.ofName("RETIRED_ARCHETYPE"))
        assertNull(Archetype.ofName(null))
    }

    @Test
    fun `평가 행의 항목 점수 JSON을 읽는다`() {
        assertEquals(BigDecimal("63.25"), ScoreComponents.fromColumn("[63.25,71,55,66]").scoreOf(ScoreDimension.CARE_EMPATHY))
    }

    private fun components(vararg values: Int): ScoreComponents = ScoreComponents(values.map { BigDecimal(it) })

    companion object {
        private const val SPLIT = 50
        private val LEVEL_NOISE = listOf(-2.5, -1.0, 0.0, 1.0, 2.5)
        private val GAP_NOISE = listOf(-2.0, -1.0, 0.0, 1.0, 2.0)
    }
}
