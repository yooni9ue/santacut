package com.santacut.domain.roast.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 일반론 검사. 문장은 근거(말풍선 표시, 칩 이름과 뜻 풀이)와 내용 낱말을 하나 이상 나눠야 한다. */
class TextProblemsTest {
    @Test
    fun `활용으로 둘째 글자가 바뀌어도 첫 글자가 같은 내용 낱말이면 통과한다`() {
        assertPasses("고맙다는 말이 품절됐다.", null, listOf("'고마워'"))
        assertPasses("물어보기만 함", null, listOf("물음표 살포율", "물음표가 든 말풍선의 비율"))
        assertPasses("새벽마다 불을 켰다.", null, listOf("새벽 출몰률"))
    }

    @Test
    fun `말풍선 낱말은 첫 글자가 그 안 어디에 있어도 통과한다`() {
        assertPasses("답장 독촉 혐의로 체포하겠어.", "빨리 대답좀", listOf("연달아 보내기", "끊지 않고 연달아 보낸 말풍선 묶음의 평균 크기"))
    }

    @Test
    fun `칩 뜻 풀이에 든 변형 낱말도 근거로 친다`() {
        assertPasses("감사 인사는 품절이다.", null, listOf("'고마워'", "고맙다는 말(고마워, 고맙다, ㄱㅅ, 감사, 땡큐)을 보낸 말풍선 수"))
    }

    @Test
    fun `근거의 따옴표 속 입버릇을 그대로 쓰면 통과한다`() {
        assertPasses("오늘도 'ㄱ?' 장전.", null, listOf("'ㄱ?' 발사 횟수"))
    }

    @Test
    fun `어느 덱에나 붙는 낱말만 쓴 문장은 일반론으로 떨어진다`() {
        val problems = TextProblems.grounding("이건 산타도 인정한다.", "빨리 대답좀", listOf("물음표 살포율", "물음표가 든 말풍선의 비율")).toList()

        assertTrue(problems.single().startsWith("일반론"))
    }

    @Test
    fun `겹치는 내용 낱말이 없으면 일반론으로 떨어진다`() {
        val problems = TextProblems.grounding("웃고 넘어갔다.", "내일 몇 시에 볼까", listOf("새벽 출몰률")).toList()

        assertTrue(problems.single().startsWith("일반론"))
    }

    @Test
    fun `근거에 내용 낱말이 없으면 보지 않는다`() {
        assertPasses("이건 산타도 인정한다.", "ㅋㅋㅋㅋ", emptyList())
        assertPasses("이건 산타도 인정한다.", null, emptyList())
    }

    private fun assertPasses(text: String, quote: String?, chipTexts: List<String>) =
        assertEquals(emptyList(), TextProblems.grounding(text, quote, chipTexts).toList(), text)
}
