package com.santacut.domain.dossier.model

import com.santacut.TestDossiers
import com.santacut.TestDossiers.selves
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoringMessage
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 조회 시점 장면 조립: 노출 구간, 담백 톤, 한마디 대체, 판결, 결정적 선택. 문구는 픽스처 리소스의 것이다. */
class DossierStoryTest {
    @Test
    fun `본인 메시지 300개 이상이면 모든 장면과 극값 3장, 장부 5줄을 보여 준다`() {
        val view = write(richMessages(400))

        assertEquals(
            listOf("teaser", "hook", "ledger", "extreme_1", "extreme_2", "extreme_3", "acts", "dossier", "verdict", "summons"),
            view.scenes.ids(),
        )
        assertEquals(5, phraseLines(view))
    }

    @Test
    fun `100개 이상 300개 미만이면 장부는 3줄, 극값은 2장이고 화제가 없는 막은 뺀다`() {
        val view = write(richMessages(150))

        assertEquals(listOf("teaser", "hook", "ledger", "extreme_1", "extreme_2", "acts", "dossier", "verdict", "summons"), view.scenes.ids())
        assertEquals(3, phraseLines(view))
        assertTrue(scene(view, "acts").lines.toList().none { line -> line.text.endsWith("빈 막") })
    }

    @Test
    fun `100개 미만이면 장부·3막·조서를 빼고 티저 맨 앞에 더 시끄러운 방 안내를 둔다`() {
        val view = write(richMessages(60))

        assertEquals(listOf("teaser", "hook", "extreme_1", "extreme_2", "verdict", "summons"), view.scenes.ids())
        assertEquals("적은 말풍선 안내", scene(view, "teaser").lines.toList()[0].text)
        assertFalse(scene(view, "teaser").shareable)
    }

    @Test
    fun `특이도 1위가 최소 횟수에 못 미치면 한마디는 ㅋ 밀도로 바꾸고 넘으면 그 말을 크게 보여 준다`() {
        val fallback = scene(write(selves(List(40) { "ㅋㅋㅋ 응" })), "hook")
        val phrase = scene(write(selves(List(40) { "헐 ㅋㅋ" })), "hook")

        assertEquals("웃음 제목", fallback.headline)
        assertEquals("100개당 300개", fallback.bigValue)
        assertEquals("헐", phrase.bigValue)
        assertEquals("40번", phrase.lines.toList()[0].text)
    }

    @Test
    fun `담백 톤이면 죄목과 놀림 문구를 빼고 극값은 지표 이름과 값에 중립 메모만 단다`() {
        val view = write(richMessages(400), tone = DossierTone.MILD)

        val caseLines = scene(view, "dossier").lines.toList().map(SceneLine::text)
        assertTrue(caseLines.none { line -> line.startsWith("죄목") }, caseLines.toString())
        assertEquals("담백 총평", caseLines.last())
        assertEquals("담백 메모", scene(view, "extreme_1").memo)
        assertEquals("무난한 말풍선 운송 기사", view.title)
        assertNull(scene(view, "verdict").lines.toList().find { line -> line.text.startsWith("주석") })
    }

    @Test
    fun `조서는 죄목 둘과 공로 둘을 서로 다른 통계로 고르고 총평으로 닫는다`() {
        val lines = scene(write(richMessages(400)), "dossier").lines.toList().map(SceneLine::text)

        assertEquals(2, lines.count { line -> line.startsWith("죄목") })
        assertTrue(lines.count { line -> line.startsWith("공로") } in 1..2)
        assertTrue(lines.last().startsWith("총평"))
    }

    @Test
    fun `판결은 최고점 항목의 따뜻한 말 횟수를 사유로 쓰고 하위 판정 줄은 카드에서 가린다`() {
        val view = write(richMessages(400), scoreHundredths = LOW_SCORE)

        val verdict = scene(view, "verdict")
        val lines = verdict.lines.toList()
        assertEquals("석탄 착불 배송", verdict.headline)
        assertEquals("사유 공감 '괜찮아?' 200번이", lines[0].text)
        assertEquals(LineDraft("하위 판정 줄", false), LineDraft(lines.last().text, lines.last().shareable))
        assertEquals("전국 12위", verdict.bigValue)
        assertEquals("종로구 · 도착 예정 12월 25일 오전 6시 12분", verdict.memo)
    }

    @Test
    fun `삐 처리 줄은 화면 전용이고 장부 순위 줄은 공유할 수 있다`() {
        val ledger = scene(write(richMessages(400) + selves(List(5) { "ㅅㅂ" })), "ledger")

        val lines = ledger.lines.toList()
        assertTrue(lines.dropLast(1).all(SceneLine::shareable))
        assertEquals(SceneLine("l${lines.size}", "삐 5번", false), lines.last())
    }

    @Test
    fun `같은 결과는 늘 같은 장면이고 결과마다 시드가 달라 조서 번호가 바뀐다`() {
        val messages = richMessages(400)

        assertEquals(write(messages), write(messages))
        assertNotNull(scene(write(messages), "dossier").headline)
    }

    private fun write(
        messages: List<ScoringMessage>,
        tone: DossierTone? = null,
        scoreHundredths: Int = HIGH_SCORE,
    ): DossierView {
        val tally = TestDossiers.tally(messages)
        val subject = DossierSubject(
            ASSESSMENT_ID, if (scoreHundredths < MID_SCORE) "석탄 착불 배송" else "선물 배송 출발", scoreHundredths, CARE_FIRST, RANK, ETA, REGION, YEAR,
        )
        return DossierStory(TestDossiers.fixture, tally.stats, tone ?: tally.tone, subject).write()
    }

    /** 입버릇 셋, 따뜻한 말, 주제, ㅋ가 섞인 본인 메시지 [count]개. 달마다 나뉘도록 하루씩 띄운다. */
    private fun richMessages(count: Int): List<ScoringMessage> =
        selves(List(count) { index -> RICH_TEXTS[index % RICH_TEXTS.size] }, minutesApart = MINUTES_PER_SIX_HOURS)

    private fun phraseLines(view: DossierView): Int = scene(view, "ledger").lines.toList().count { line -> line.text.contains("위 ") }

    private fun scene(view: DossierView, id: String): DossierScene = view.scenes.find(id) ?: error("장면 $id 없음")

    companion object {
        private const val ASSESSMENT_ID = "assessment-1"
        private const val HIGH_SCORE = 8000
        private const val MID_SCORE = 5000
        private const val LOW_SCORE = 3000
        private const val RANK = 12
        private const val REGION = "종로구"
        private const val YEAR = 2026
        private const val MINUTES_PER_SIX_HOURS = 360L
        private val ETA: Instant = Instant.parse("2026-12-24T21:12:00Z")
        private val CARE_FIRST = ScoreComponents(listOf(90, 60, 60, 60).map(::BigDecimal))
        private val RICH_TEXTS = listOf(
            "진짜 ㅋㅋㅋ 괜찮아? 밥 먹자 대박", "ㅇㅋ 진짜 고마워 여행 가자 근데", "헐 진짜 ㅋㅋ 조심히 와 아니", "ㅇㅋ 치킨 ㅋㅋㅋㅋ 같이 가 대박",
        )
    }
}
