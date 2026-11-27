package com.santacut.domain.roast.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 일반론 검사. 문장은 근거(말풍선 표시, 칩 이름)와 내용 낱말을 하나 이상 나눠야 한다. */
class TextProblemsTest {
    @Test
    fun `두 글자 이상 한글 낱말의 줄기가 겹치면 통과한다`() {
        assertEquals(emptyList(), TextProblems.grounding("새벽마다 말풍선을 켰다.", listOf("새벽 출몰률")).toList())
        assertEquals(emptyList(), TextProblems.grounding("내일은 또 언제냐.", listOf("내일 몇 시에 볼까")).toList())
    }

    @Test
    fun `근거의 따옴표 속 입버릇을 그대로 쓰면 통과한다`() {
        assertEquals(emptyList(), TextProblems.grounding("오늘도 'ㄱ?' 장전.", listOf("'ㄱ?' 발사 횟수")).toList())
    }

    @Test
    fun `겹치는 내용 낱말이 없으면 일반론으로 떨어진다`() {
        val problems = TextProblems.grounding("산타도 웃고 넘어갔다.", listOf("새벽 출몰률", "내일 몇 시에 볼까")).toList()

        assertTrue(problems.single().startsWith("일반론"))
    }

    @Test
    fun `근거에 내용 낱말이 없으면 보지 않는다`() {
        assertEquals(emptyList(), TextProblems.grounding("산타도 웃고 넘어갔다.", listOf("ㅋㅋㅋㅋ")).toList())
        assertEquals(emptyList(), TextProblems.grounding("산타도 웃고 넘어갔다.", emptyList()).toList())
    }
}
