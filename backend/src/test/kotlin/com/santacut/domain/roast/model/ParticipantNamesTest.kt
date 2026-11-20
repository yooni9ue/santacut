package com.santacut.domain.roast.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 참여자 표시 이름에서 가릴 이름 만들기. */
class ParticipantNamesTest {
    @Test
    fun `표시 이름 그대로와 끝 두 글자 이름을 넣고 한 글자 이름은 뺀다`() {
        val names = ParticipantNames.of(listOf("평친성필", "평친동준", "윤현식", "나")).toList()

        assertTrue(names.containsAll(listOf("평친성필", "성필", "평친동준", "동준", "윤현식", "현식")))
        assertTrue("나" !in names)
    }

    @Test
    fun `참여자 모두에게 붙은 공통 머리말은 떼어 낸 이름도 넣는다`() {
        assertEquals(listOf("평친성필", "성필", "평친동준", "동준"), ParticipantNames.of(listOf("평친성필", "평친동준")).toList())
    }
}
