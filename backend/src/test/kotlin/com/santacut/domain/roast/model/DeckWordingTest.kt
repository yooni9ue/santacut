package com.santacut.domain.roast.model

import com.santacut.TestRoastAnswers.chipWordOf
import com.santacut.TestRoastAnswers.evidenceOf
import com.santacut.TestRoastAnswers.habitMessages
import com.santacut.TestRoastAnswers.review
import com.santacut.TestRoastAnswers.substantialLineIds
import com.santacut.TestRoastAnswers.validJson
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.ScoreComponents
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 공유 카드 문장 다듬기 규칙. 근거 한 줄은 10자 이상의 완결된 찌르기 문장이고, 라벨은 말풍선·앞 라벨·판정 한 줄을 되풀이하지 않으며,
 * 카드끼리 같은 낱말을 되풀이하지 않는다. 칭호는 서술형이 아니고, 다섯 장 덱의 대표 말은 근거 카드 말과 다르다.
 */
class DeckWordingTest {
    private val rules = TestRoasts.catalog.rules
    private val brief = TestRoasts.briefWithoutPunches(habitMessages())

    @Test
    fun `회귀 - 조심히 가라고 챙김, 들어줘서 따뜻함, 미안 늦게 봄은 밋밋한 근거 한 줄로 떨어진다`() {
        listOf("조심히 가라고 챙김", "들어줘서 따뜻함", "미안 늦게 봄", "좋은 카페 찾아 방 띄움", "언제 가능 물으며 방을 독촉함.").forEach { line ->
            val problems = TextProblems.of(line, RoastTextKind.EVIDENCE_LINE, brief, rules).toList()
            assertTrue(problems.any { reason -> reason.startsWith("끝이 밋밋하다") || reason.startsWith("글자 수") }, "$line $problems")
        }
        assertTrue(TextProblems.isFlatEnding("조심히 가라고 챙김", rules))
        assertTrue(TextProblems.isFlatEnding("약속 잡자마자 먼저 출발하는 타입.", rules))
        assertTrue(TextProblems.isFlatEnding("빨리 대답하라며 방을 볶는 버릇.", rules))
    }

    @Test
    fun `근거 한 줄은 10자 이상이고 완결된 문장 끝이나 유행어로 맺으면 통과한다`() {
        assertTrue(TextProblems.of("미안 늦게 봤어.", RoastTextKind.EVIDENCE_LINE, brief, rules).toList().any { reason -> reason.startsWith("글자 수") })
        listOf("약속은 늘 네 물음표에서 시작한다.", "새벽 라면 호출은 이제 전통이다.", "답장 독촉 현행범이다.", "질문으로 판 깔기, 요이~", "물음표만 던지고 사라지냐?")
            .forEach { line -> assertFalse(TextProblems.isFlatEnding(line, rules), line) }
        assertTrue(TextProblems.isFlatEnding("약속은 늘 'ㄱ?'", rules))
        assertTrue(TextProblems.isFlatEnding("약속 시작은 늘 '빨리'.", rules), "따옴표 인용으로 끝나면 밋밋하다")
        assertFalse(TextProblems.isFlatEnding("'빨리' 소리가 또 들린다.", rules))
        assertTrue(TextProblems.isFlatEnding("질문으로 판 깔기.", rules))
    }

    @Test
    fun `회귀 - 말풍선을 그대로 옮긴 라벨은 떨어진다`() {
        assertEquals(listOf("라벨이 말풍선을 그대로 옮겼다(살았다)"), TextProblems.copiedLabel("살았다상", "상", "고마워 덕분에 살았다").toList())
        assertEquals(emptyList(), TextProblems.copiedLabel("감사고백상", "상", "고마워 진짜").toList())
        assertEquals(emptyList(), TextProblems.copiedLabel("살았다상", "상", null).toList())
    }

    @Test
    fun `라벨 첫머리가 앞 라벨과 같거나 판정 한 줄을 옮기면 떨어진다`() {
        assertEquals(
            listOf("앞 카드 라벨과 첫머리가 같다(답장독)"),
            TextProblems.sharedLabel("답장독촉왕상", listOf("답장독촉상"), "", "상").toList(),
        )
        assertEquals(
            listOf("판정 한 줄을 그대로 옮긴 라벨(답장독촉)"),
            TextProblems.sharedLabel("답장독촉상", emptyList(), "답장 독촉 혐의로 체포하겠어.", "상").toList(),
        )
        assertEquals(emptyList(), TextProblems.sharedLabel("약속발사상", listOf("감사품절상"), "선물 상자 탑승 완료, 딸깍.", "상").toList())
        assertEquals(emptyList(), TextProblems.sharedLabel("답장지연상", listOf("답장독촉상"), "", "상").toList(), "두 음절만 같으면 통과한다")
    }

    @Test
    fun `회귀 - 세 글자 이상 낱말을 두 카드에, 짧은 낱말을 세 카드에 되풀이하면 뒤 카드가 떨어진다`() {
        val first = "빨리 대답하라며 방을 볶는 버릇."
        val second = "뭐함 질문으로 단톡을 깨우는 버릇."
        assertEquals(emptyList(), TextProblems.repeated(second, listOf(first), emptyList()).toList(), "짧은 낱말은 두 카드까지 괜찮다")
        assertEquals(
            listOf("앞 카드와 같은 낱말(버릇)"),
            TextProblems.repeated("새벽마다 알림 켜는 버릇.", listOf(first, second), emptyList()).toList(),
        )
        assertEquals(
            listOf("앞 카드와 같은 낱말(현행범이다)"),
            TextProblems.repeated("물음표 살포 현행범이다.", listOf("답장 독촉 현행범이다."), emptyList()).toList(),
        )
        assertEquals(
            emptyList(),
            TextProblems.repeated("진짜 오늘도 수준이다.", listOf("진짜 오늘도 수준이다.", "진짜 오늘도 수준이다."), emptyList()).toList(),
            "흔한 말은 되풀이로 치지 않는다",
        )
        assertEquals(emptyList(), TextProblems.repeated("'수고' 발사에 귀가 닳았다.", listOf("'수고' 소리가 또 들린다."), emptyList()).toList())
        assertEquals(emptyList(), TextProblems.repeated("발사 횟수로 판을 깔았다.", listOf("발사 횟수가 또 늘었다."), listOf("'빨리' 발사 횟수")).toList())

        val cards = evidenceOf(brief).mapIndexed { index, card -> if (index == 1) card + ("line" to "${card["line"]}".replace(".", "") + " 발사로 방이 굴러갔다.") else card }
        val checks = review(brief, validJson(brief, evidence = cards)).checks
        assertTrue(checks.reasonsFor(listOf(EvidenceDrafts.FIELD)).any { reason -> reason.contains("앞 카드와 같은 낱말") }, checks.toList().toString())
    }

    @Test
    fun `서술형 칭호는 떨어지고 밈형 명사구는 통과한다`() {
        listOf("독촉함", "답장 안 하는 사람", "약속 잘 잡는다", "다정왕?").forEach { title -> assertTrue(TextProblems.isDescriptiveTitle(title), title) }
        listOf("약속발사대", "독촉의신", "다정왕", "'ㄱ?'장인", "물음표 총무").forEach { title -> assertFalse(TextProblems.isDescriptiveTitle(title), title) }
        assertTrue(TextProblems.of("답장 안 하는 사람", RoastTextKind.TITLE, brief, rules).toList().any { reason -> reason.startsWith("칭호가 서술형이다") })
    }

    @Test
    fun `다섯 장 덱이면 대표 말을 근거 카드와 다른 줄로 골라야 한다`() {
        val short = TestRoasts.briefWithoutPunches(habitMessages(), TestRoasts.COAL_SCORE, ONE_WEAK)
        assertTrue(short.slots.total() <= RoastEvidences.MIN_CARDS, short.slots.toString())
        val cards = evidenceOf(short)
        val usedLine = cards.first { card -> card["line_id"] != null }["line_id"] as Int
        val usedChip = cards.first { card -> card["line_id"] == usedLine }["chip_key"] as String
        val freshLine = substantialLineIds(short).last { id -> id !in cards.map { card -> card["line_id"] } && short.quoteFits(usedChip, id) }

        val repeated = review(short, validJson(short, evidence = cards, heroLineId = usedLine, heroChipKey = usedChip, finalLine = "${chipWord(short, usedChip)} 장부 결산, 요이~")).checks
        val fresh = review(short, validJson(short, evidence = cards, heroLineId = freshLine, heroChipKey = usedChip, finalLine = "${chipWord(short, usedChip)} 장부 결산, 요이~")).checks

        assertTrue(repeated.reasonsFor(listOf(ConclusionDraft.FIELD)).any { reason -> reason.contains("다섯 장 덱이라 hero_line_id") })
        assertFalse(fresh.reasonsFor(listOf(ConclusionDraft.FIELD)).any { reason -> reason.contains("다섯 장 덱이라") })
    }

    @Test
    fun `대표 말풍선은 대표 칩의 말풍선 조건에 맞아야 하고 말풍선 없이 쓰는 대표 칩이면 대표 말을 비운다`() {
        val quoteless = brief.facts.keys().first { key -> brief.quoteRuleOf(key) == QuoteRule.NONE && key != ThanksFactSpec.KEY }
        val (quoted, heroLine) = brief.facts.keys().filter { key -> brief.quoteRuleOf(key) != QuoteRule.NONE }.firstNotNullOf { key ->
            substantialLineIds(brief).lastOrNull { id -> !brief.quoteFits(key, id) }?.let { id -> key to id }
        }
        val finalLine = "${chipWordOf(brief, quoteless)} 장부 결산, 요이~"

        val mismatched = review(brief, validJson(brief, heroLineId = heroLine, heroChipKey = quoted)).checks
        val withQuote = review(brief, validJson(brief, heroLineId = heroLine, heroChipKey = quoteless, finalLine = finalLine)).checks
        val chipOnly = review(brief, validJson(brief, heroLineId = null, heroChipKey = quoteless, finalLine = finalLine)).checks

        assertTrue(mismatched.reasonsFor(listOf(ConclusionDraft.FIELD)).any { reason -> reason.startsWith("conclusion 대표 말풍선이 대표 칩과 맞지 않는다") })
        assertTrue(withQuote.reasonsFor(listOf(ConclusionDraft.FIELD)).any { reason -> reason.contains("hero_line_id를 null로 둔다") })
        assertTrue(chipOnly.passed(ConclusionDraft.FIELD), chipOnly.reasonsFor(listOf(ConclusionDraft.FIELD)).toString())
    }

    private fun chipWord(target: RoastBrief, chipKey: String): String = chipWordOf(target, chipKey)

    companion object {
        /** 컷 아래 항목이 하나뿐인 항목 점수. 석탄 덱이 깎인 말 한 장·살아남은 말 한 장의 다섯 장 덱이 된다. */
        private val ONE_WEAK: ScoreComponents = ScoreComponents.fromColumn("[90,90,90,50]")
    }
}
