package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 참여자 표시 이름에서 가릴 이름 만들기. */
class ParticipantNamesTest {
    private val rules = TestRoasts.catalog.rules

    @Test
    fun `표시 이름 그대로와 끝 두 글자 이름을 넣고 한 글자 이름은 뺀다`() {
        val names = rules.participantNames(listOf("평친성필", "평친동준", "윤현식", "나")).toList()

        assertTrue(names.containsAll(listOf("평친성필", "성필", "평친동준", "동준", "윤현식", "현식")))
        assertFalse("나" in names)
    }

    @Test
    fun `직함이 붙은 이름은 성을 뗀 이름을 넣고 직함은 넣지 않는다`() {
        val names = rules.participantNames(listOf("김민수 대리", "김민지", "나")).toList()

        assertTrue(names.containsAll(listOf("김민수", "민수", "김민지", "민지")))
        assertFalse("대리" in names)
    }

    @Test
    fun `이모지·숫자·영문이 붙은 표시 이름에서도 한글 이름 덩이를 넣고 한글 음절이 없는 이름은 뺀다`() {
        val names = rules.participantNames(listOf("민수❤", "지은2", "현규 Kim", "ㅇㅇ", "Kim")).toList()

        assertTrue(names.containsAll(listOf("민수", "지은", "현규")))
        assertFalse(names.any { name -> name == "ㅇㅇ" || name == "Kim" })
    }

    @Test
    fun `공통 머리말은 뗀 뒤 두 글자 넘게 남을 때만 떼어 같은 돌림자 형제 이름을 잃지 않는다`() {
        val names = rules.participantNames(listOf("김민수", "김민지")).toList()

        assertTrue(names.containsAll(listOf("민수", "민지")))
        assertTrue(rules.participantNames(listOf("평친성필", "평친동준")).toList().containsAll(listOf("성필", "동준")))
    }
}
