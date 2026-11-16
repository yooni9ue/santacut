package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvidenceIndicesTest {
    @Test
    fun `모든 항목의 근거 목록이 들어와야 완전하고 빈 목록도 받는다`() {
        assertTrue(EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3, 4), listOf(5))).isComplete())
        assertTrue(EvidenceIndices(listOf(listOf(1), emptyList(), listOf(3), listOf(4))).isComplete())
        assertFalse(EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3))).isComplete())
    }

    @Test
    fun `위치로 항목 근거를 꺼내고 없는 위치는 빈 목록이다`() {
        val evidence = EvidenceIndices(listOf(emptyList(), listOf(2), emptyList(), listOf(4)))

        assertEquals(listOf(2), evidence.at(1))
        assertEquals(emptyList(), evidence.at(9))
    }

    @Test
    fun `줄 범위 밖 번호와 항목 안에서 겹친 번호를 지운다`() {
        val evidence = EvidenceIndices(listOf(listOf(1, 1, 21), listOf(0, 2), listOf(-1), listOf(20, 3, 20)))

        assertEquals(emptyList(), evidence.cleaned(1..20).at(2))
        assertEquals(5, evidence.discardedCount(1..20))
    }

    @Test
    fun `구간 줄 범위가 1부터 시작하지 않아도 그 범위로 거른다`() {
        assertEquals(listOf(41), EvidenceIndices(listOf(listOf(3, 41), listOf(), listOf(), listOf())).cleaned(40..60).at(0))
    }

    @Test
    fun `지울 번호가 없으면 0개다`() {
        assertEquals(0, EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3, 4), listOf(20))).discardedCount(1..20))
    }
}
