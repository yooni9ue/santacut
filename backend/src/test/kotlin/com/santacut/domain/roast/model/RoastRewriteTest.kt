package com.santacut.domain.roast.model

import com.santacut.TestRoastAnswers.chipWordOf
import com.santacut.TestRoastAnswers.evidenceOf
import com.santacut.TestRoastAnswers.finalLineFor
import com.santacut.TestRoastAnswers.habitMessages
import com.santacut.TestRoastAnswers.heroOf
import com.santacut.TestRoastAnswers.review
import com.santacut.TestRoastAnswers.validJson
import com.santacut.TestRoastAnswers.verdictLineFor
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 여러 응답을 모으는 동안의 다시 쓰기 요청. 받아 둔 문장(kept)을 작가에게 보여 주고, 검증은 통과했지만 받아 둔 문장과 겹쳐
 * 받지 않은 묶음·카드의 사유를 싣는다. 사유 없이 버려 작가가 같은 실수로 호출을 다 쓰는 일을 막는다.
 */
class RoastRewriteTest {
    private val brief = TestRoasts.brief(habitMessages())
    private val rules = TestRoasts.catalog.rules
    private val verdict = verdictLineFor(brief)

    @Test
    fun `다시 쓰기 요청은 받아 둔 판정 한 줄과 근거 카드를 kept로 싣는다`() {
        val first = review(brief, validJson(brief, title = "두 배 장인"))
        val sheet = RoastSheet.EMPTY.absorb(first, brief, rules)

        val rewrite = sheet.rewriteFor(first)
        val kept = assertNotNull(rewrite.kept)

        assertEquals(verdict, kept.verdictLine)
        assertNull(kept.finalLine)
        val cards = evidenceOf(brief)
        assertEquals(cards.map { card -> card["chip_key"] }, kept.evidence.map(KeptCard::chipKey))
        assertEquals(cards.map { card -> card["line_id"] }, kept.evidence.map(KeptCard::lineId))
        assertEquals(cards.map { card -> card["label"] }, kept.evidence.map(KeptCard::label))
        val input = brief.inputText(TestRoasts.mapper, rewrite)
        assertTrue(input.contains("\"kept\":{\"verdict_line\":\"$verdict\",\"evidence\":[{\"kind\":\"reason\""), input)
    }

    @Test
    fun `받아 둔 판정 한 줄과 펀치가 겹쳐 버린 결론은 사유를 다시 쓰기 요청에 싣는다`() {
        val loose = TestRoasts.briefWithoutPunches(habitMessages())
        val sheet = RoastSheet.EMPTY.absorb(review(loose, validJson(loose, verdictLine = "올해 장부 결산, 딸깍.", title = "두 배 장인")), loose, rules)
        val clashing = finalLineFor(loose, heroOf(loose).second, "").replace("요이~", "딸깍.")
        val second = review(loose, validJson(loose, verdictLine = "다른 판정 한 줄이다.", finalLine = clashing))

        val after = sheet.absorb(second, loose, rules)

        assertEquals(listOf(ConclusionDraft.FIELD), after.missingFields())
        val reasons = after.rewriteFor(second).reasons
        assertTrue(reasons.contains("conclusion 판정 한 줄과 결론 마지막 한 줄이 같은 펀치를 써 받지 않았다(딸깍)"), reasons.toString())
    }

    @Test
    fun `받아 둔 카드와 낱말이 겹쳐 버린 근거 카드는 사유를 다시 쓰기 요청에 싣는다`() {
        val cards = evidenceOf(brief)
        val broken = cards.mapIndexed { index, card -> if (index == 1) card + ("line" to "물음표를 세 번 보냈다.") else card }
        val sheet = RoastSheet.EMPTY.absorb(review(brief, validJson(brief, evidence = broken)), brief, rules)
        val repeating = cards[1] + ("line" to "${chipWordOf(brief, cards[1]["chip_key"] as String)} 발사로 판을 깔았다.")
        val second = review(brief, validJson(brief, evidence = listOf(repeating)))

        val after = sheet.absorb(second, brief, rules)

        assertEquals(listOf(EvidenceDrafts.FIELD), after.missingFields())
        val reasons = after.rewriteFor(second).reasons
        assertTrue(reasons.any { reason -> reason.startsWith("evidence[0] 이미 받은 카드와 겹쳐 받지 않았다: line: 앞 카드와 같은 낱말(발사로)") }, reasons.toString())
    }

    @Test
    fun `이미 받은 카드를 그대로 다시 내면 사유 없이 건너뛴다`() {
        val broken = evidenceOf(brief).mapIndexed { index, card -> if (index == 1) card + ("line" to "물음표를 세 번 보냈다.") else card }
        val sheet = RoastSheet.EMPTY.absorb(review(brief, validJson(brief, evidence = broken)), brief, rules)
        val again = review(brief, validJson(brief))

        val after = sheet.absorb(again, brief, rules)

        assertTrue(after.isComplete())
        assertTrue(after.rewriteFor(again).reasons.none { reason -> reason.contains("이미 받은 카드와 겹쳐") })
    }

    @Test
    fun `앞 카드가 제 검증에 떨어지면 뒤 카드는 그 카드와 견주지 않는다`() {
        val cards = evidenceOf(brief)
        val word = chipWordOf(brief, cards[1]["chip_key"] as String)
        val failing = cards[0] + ("line" to "발사로 세 번 터졌다.")
        val echoing = cards[1] + ("line" to "$word 발사로 판을 깔았다.")
        val checks = review(brief, validJson(brief, evidence = listOf(failing, echoing) + cards.drop(2))).checks

        assertTrue(!checks.passed(EvidenceDrafts.fieldOf(0)))
        assertTrue(checks.passed(EvidenceDrafts.fieldOf(1)), checks.reasonsFor(listOf(EvidenceDrafts.FIELD)).toString())
    }
}
