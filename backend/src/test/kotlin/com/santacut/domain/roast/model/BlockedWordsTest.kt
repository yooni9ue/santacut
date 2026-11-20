package com.santacut.domain.roast.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 출력 차단 목록은 입력 가리기와 같은 낱말 앞 경계로 맞춘다. */
class BlockedWordsTest {
    @Test
    fun `차단 낱말은 낱말 앞 경계에서만 걸리고 낱말 속 글자는 막지 않는다`() {
        val blocked = BlockedWords(listOf("하나", "사랑"))

        assertEquals("하나", blocked.foundIn("하나랑 놀던 손을 봤다"))
        assertNull(blocked.foundIn("두하나 짝사랑 같은 말은 괜찮다"))
    }

    @Test
    fun `긴 낱말부터 맞추고 정규식 특수 문자가 든 낱말도 글자 그대로 맞춘다`() {
        assertEquals("현규형", BlockedWords(listOf("현규", "현규형")).foundIn("현규형 타령"))
        assertEquals("a.b", BlockedWords(listOf("a.b")).foundIn("x a.b y"))
        assertNull(BlockedWords(listOf("a.b")).foundIn("axb"))
        assertNull(BlockedWords.EMPTY.foundIn("아무 말"))
    }
}
