package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvidenceIndicesTest {
    @Test
    fun `모든 항목에 근거 번호가 있어야 완전하다`() {
        assertTrue(EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3, 4), listOf(5))).isComplete())
        assertFalse(EvidenceIndices(listOf(listOf(1), emptyList(), listOf(3), listOf(4))).isComplete())
        assertFalse(EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3))).isComplete())
    }

    @Test
    fun `범위 밖 번호와 항목 안에서 겹친 번호를 지운다`() {
        val evidence = EvidenceIndices(listOf(listOf(1, 1, 21), listOf(0, 2), listOf(-1), listOf(20, 3, 20)))

        assertFalse(evidence.cleaned(20).isComplete())
        assertEquals(5, evidence.discardedCount(20))
        assertTrue(EvidenceIndices(listOf(listOf(1, 21), listOf(2), listOf(3, 3), listOf(4))).cleaned(20).isComplete())
    }

    @Test
    fun `지울 번호가 없으면 0개다`() {
        assertEquals(0, EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3, 4), listOf(20))).discardedCount(20))
    }
}
