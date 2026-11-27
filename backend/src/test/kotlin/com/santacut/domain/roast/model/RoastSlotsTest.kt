package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 근거 카드 슬롯 수. 항목 점수로 정하고 본인 줄과 칩 후보 수로 줄인다. */
class RoastSlotsTest {
    @Test
    fun `선물 덱은 컷 이상 항목 수만큼 이유, 컷 아래 항목 수만큼 깎인 말이다`() {
        assertEquals(RoastSlots(3, 1, 0), RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn("[90,80,70,60]")))
        assertEquals(RoastSlots(2, 2, 0), RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn("[90,80,60,50]")))
        assertEquals(RoastSlots(1, 2, 0), RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn("[90,60,50,40]")))
        assertEquals(RoastSlots(1, 2, 0), RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn("[60,60,60,60]")))
    }

    @Test
    fun `모든 항목이 컷 이상인 선물 덱도 깎인 말 한 장을 받는다`() {
        assertEquals(RoastSlots(3, 1, 0), RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn("[90,80,70,66]")))
        assertEquals(RoastSlots(3, 1, 0), RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn("[65,65,65,65]")))
    }

    @Test
    fun `선물 덱 근거 카드는 다섯 장을 넘지 않아 덱이 여덟 장 안이다`() {
        listOf("[90,80,70,66]", "[90,80,60,50]", "[90,60,50,40]", "[60,60,60,60]").forEach { column ->
            val slots = RoastSlots.of(GiftVerdict(GIFT), ScoreComponents.fromColumn(column))
            assertTrue(slots.total() <= RoastSlots.MAX_CARDS, column)
            assertTrue(slots.cut in 1..2, column)
        }
    }

    @Test
    fun `선물 덱은 칩이 둘 이상이면 깎인 말 몫 칩 하나를 남긴다`() {
        assertEquals(RoastSlots(2, 1, 0), RoastSlots(3, 1, 0).limitedTo(lineCount = 9, chipCount = 3))
        assertEquals(RoastSlots(3, 1, 0), RoastSlots(3, 2, 0).limitedTo(lineCount = 9, chipCount = 4))
        assertEquals(RoastSlots(1, 1, 0), RoastSlots(3, 1, 0).limitedTo(lineCount = 1, chipCount = 2))
    }

    @Test
    fun `석탄 덱은 컷 아래 항목 수만큼 깎인 말(최대 셋)과 살아남은 말 하나다`() {
        assertEquals(RoastSlots(0, 3, 1), RoastSlots.of(GiftVerdict(COAL), ScoreComponents.fromColumn("[40,40,40,40]")))
        assertEquals(RoastSlots(0, 1, 1), RoastSlots.of(GiftVerdict(COAL), ScoreComponents.fromColumn("[90,80,70,40]")))
    }

    @Test
    fun `본인 줄과 칩이 모자라면 덱의 주 카드를 먼저 남기고 줄인다`() {
        assertEquals(RoastSlots(1, 0, 0), RoastSlots(3, 2, 0).limitedTo(lineCount = 1, chipCount = 1))
        assertEquals(RoastSlots(0, 2, 0), RoastSlots(3, 2, 0).limitedTo(lineCount = 0, chipCount = 5))
        assertEquals(RoastSlots(0, 2, 0), RoastSlots(0, 3, 1).limitedTo(lineCount = 0, chipCount = 2))
        assertEquals(RoastSlots(0, 3, 1), RoastSlots(0, 3, 1).limitedTo(lineCount = 4, chipCount = 9))
    }

    companion object {
        private const val GIFT = 7373
        private const val COAL = 4200
    }
}
