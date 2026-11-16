package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ArchetypeCountsTest {
    @Test
    fun `유형 비율은 전체 참가자 수에 대한 반올림 백분율이고 참가자가 없으면 0이다`() {
        val counts = ArchetypeCounts(mapOf(Archetype.WORKSHOP_ELF to 1, Archetype.CAROL_LEADER to 2))

        assertEquals(33, counts.sharePercentOf(Archetype.WORKSHOP_ELF))
        assertEquals(67, counts.sharePercentOf(Archetype.CAROL_LEADER))
        assertEquals(0, counts.sharePercentOf(Archetype.GIFT_WRAPPER))
        assertEquals(0, ArchetypeCounts(emptyMap()).sharePercentOf(Archetype.WORKSHOP_ELF))
    }

    @Test
    fun `저장한 값을 다시 읽으면 같은 비율이고 모르는 코드와 숫자가 아닌 칸은 0명이다`() {
        val stored = ArchetypeCounts(mapOf(Archetype.WORKSHOP_ELF to 1, Archetype.CAROL_LEADER to 3)).toColumn()
        val read = ArchetypeCounts.fromColumn("$stored,UNKNOWN:5,GIFT_WRAPPER:x")

        assertEquals(25, read.sharePercentOf(Archetype.WORKSHOP_ELF))
        assertEquals(75, read.sharePercentOf(Archetype.CAROL_LEADER))
        assertEquals(0, ArchetypeCounts.fromColumn("").sharePercentOf(Archetype.CAROL_LEADER))
    }
}
