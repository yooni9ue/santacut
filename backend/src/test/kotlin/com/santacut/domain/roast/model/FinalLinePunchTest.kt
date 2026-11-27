package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftGrade
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 결론 마지막 한 줄은 밈 펀치다. 허용 유행어나 구간 펀치 틀 표지가 있어야 하고 8자 이상 22자 이하다. 추천 틀 검사는 [PunchTemplatesTest]가 본다. */
class FinalLinePunchTest {
    private val rules = TestRoasts.catalog.rules
    private val gift = TestRoasts.briefWithoutPunches(TestDossiers.selves(List(MESSAGES) { "다음 주말에 같이 놀러 가자" }))
    private val coal = TestRoasts.briefWithoutPunches(TestDossiers.selves(List(MESSAGES) { "다음 주말에 같이 놀러 가자" }), TestRoasts.COAL_SCORE)

    @Test
    fun `유행어나 펀치 틀 표지가 든 선언형 한 줄은 펀치다`() {
        listOf(
            "올해 너는 'ㄱ?'이야~",
            "답장 독촉 혐의로 체포하겠어.",
            "주말엔 산타도 못 찾음. L을 가져가.",
            "'ㄱㄱ'로 굴뚝 프리패스. 요이~",
            "약속 잡기, 딸깍.",
            "올해의 약속 총무는 너야.",
            "독촉 때문에 명단 퇴사할게여.",
        ).forEach { line -> assertTrue(rules.isPunch(line), line) }
    }

    @Test
    fun `회귀 - 조언·설명형 마지막 한 줄은 밈 펀치가 아니라 떨어진다`() {
        listOf("고마워 넘치는 따뜻한 말.", "강릉 약속 잘 잡으니 선물 챙겨 가.", "'빨리'를 적게 써라.").forEach { line ->
            assertFalse(rules.isPunch(line), line)
            assertTrue(TextProblems.of(line, RoastTextKind.FINAL_LINE, gift, rules).toList().any { reason -> reason.startsWith("밈 펀치가 아니다") }, line)
        }
    }

    @Test
    fun `마지막 한 줄은 8자 이상 22자 이하다`() {
        assertTrue(TextProblems.of("요이~", RoastTextKind.FINAL_LINE, gift, rules).toList().any { reason -> reason.startsWith("글자 수") })
        assertTrue(TextProblems.of("약속은 네가 잡고 선물은 산타가 잡는다, 딸깍.", RoastTextKind.FINAL_LINE, gift, rules).toList().any { reason -> reason.startsWith("글자 수") })
        assertEquals(emptyList(), TextProblems.of("약속 잡기, 딸깍.", RoastTextKind.FINAL_LINE, gift, rules).toList())
    }

    @Test
    fun `구간별 대체 마지막 한 줄은 유행어가 든 것도 없는 것도 모두 펀치 검사를 통과한다`() {
        GiftGrade.entries.forEach { grade ->
            val band = TestRoasts.catalog.fallback.bandOf(grade)
            val brief = if (grade.ordinal <= GiftGrade.SCRAPED_THROUGH.ordinal) gift else coal
            listOf(band.finalLine, band.plainFinalLine).forEach { line ->
                assertTrue(rules.isPunch(line), "${grade.name} $line")
                assertEquals(emptyList(), TextProblems.of(line, RoastTextKind.FINAL_LINE, brief, rules).toList(), "${grade.name} $line")
            }
            assertEquals(0, rules.slangCountIn(band.plainFinalLine), grade.name)
        }
    }

    companion object {
        private const val MESSAGES = 60
    }
}
