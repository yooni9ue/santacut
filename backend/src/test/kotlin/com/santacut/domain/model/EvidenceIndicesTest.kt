package com.santacut.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EvidenceIndicesTest {
    @Test
    fun `모든 항목에 근거 번호가 있어야 완전하다`() {
        assertTrue(EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3, 4), listOf(5))).isComplete())
        assertFalse(EvidenceIndices(listOf(listOf(1), emptyList(), listOf(3), listOf(4))).isComplete())
        assertFalse(EvidenceIndices(listOf(listOf(1), listOf(2), listOf(3))).isComplete())
    }

    @Test
    fun `이벤트 범위를 벗어난 첫 근거 번호를 찾는다`() {
        assertNull(EvidenceIndices(listOf(listOf(1), listOf(20))).firstOutOfRange(20))
        assertEquals(0, EvidenceIndices(listOf(listOf(1), listOf(0), listOf(21))).firstOutOfRange(20))
        assertEquals(21, EvidenceIndices(listOf(listOf(1, 21))).firstOutOfRange(20))
    }
}
