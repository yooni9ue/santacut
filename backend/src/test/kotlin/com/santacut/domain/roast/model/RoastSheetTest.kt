package com.santacut.domain.roast.model

import com.santacut.TestRoastAnswers.evidenceOf
import com.santacut.TestRoastAnswers.habitMessages
import com.santacut.TestRoastAnswers.review
import com.santacut.TestRoastAnswers.validJson
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftGrade
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 통과한 묶음 모으기, 다시 쓰기 요청, 구간별 대체 문장과 칩 통계 카드 채우기. */
class RoastSheetTest {
    private val brief = TestRoasts.brief(habitMessages())
    private val rules = TestRoasts.catalog.rules
    private val fallback = TestRoasts.catalog.fallback

    @Test
    fun `통과한 묶음은 지키고 떨어진 묶음만 다시 쓰기 요청에 사유와 함께 싣는다`() {
        val first = review(brief, validJson(brief, title = "두 배 장인"))
        val sheet = RoastSheet.EMPTY.absorb(first, rules)

        assertEquals(listOf(ConclusionDraft.FIELD), sheet.missingFields())
        val rewrite = sheet.rewriteFor(first)
        assertEquals(listOf(ConclusionDraft.FIELD), rewrite.fields)
        assertTrue(rewrite.reasons.any { reason -> reason.startsWith("conclusion title: 숫자 표현") })

        val fixed = sheet.absorb(review(brief, validJson(brief, verdictLine = "바뀌면 안 되는 한 줄이다.")), rules)
        assertTrue(fixed.isComplete())
        val content = fixed.toContent(brief, fallback, rules)
        assertEquals(RoastText("올해 약속 발사대, 딸깍.", RoastSource.AI), content.verdictLine)
        assertEquals(0, content.fallbackCount())
    }

    @Test
    fun `통과한 근거 카드에는 서버가 본인 말풍선과 칩을 채운다`() {
        val content = RoastSheet.EMPTY.absorb(review(brief, validJson(brief)), rules).toContent(brief, fallback, rules)
        val first = content.evidence.toList().first()
        val chosen = evidenceOf(brief).first()

        assertEquals(EvidenceKind.REASON, first.kind)
        assertEquals(brief.quoteOf(chosen["line_id"] as Int), first.quote)
        assertEquals(brief.chipOf(chosen["chip_key"] as String), first.chip)
        assertEquals("약속발사상", first.label)
        assertNotNull(content.conclusion?.heroQuote)
    }

    @Test
    fun `읽을 수 없는 응답은 아무것도 채우지 않는다`() {
        val sheet = RoastSheet.EMPTY.absorb(review(brief, "not json"), rules)

        assertEquals(3, sheet.missingFields().size)
    }

    @Test
    fun `끝까지 빈 묶음은 구간별 고정 문장과 칩 통계 카드로 채운다`() {
        val content = RoastSheet.EMPTY.toContent(brief, fallback, rules)
        val band = fallback.bandOf(GiftGrade.of(TestRoasts.GIFT_SCORE))

        assertEquals(RoastText(band.verdictLine, RoastSource.FALLBACK), content.verdictLine)
        assertEquals(brief.slots.total(), content.evidence.size())
        assertTrue(content.evidence.toList().all { card -> card.quote == null && card.chip != null && card.source == RoastSource.FALLBACK })
        assertEquals(band.title, content.conclusion?.title?.text)
        assertEquals(content.evidence.toList().first().chip, content.conclusion?.heroChip)
    }

    @Test
    fun `작가 문장과 합쳐 덱 전체 유행어가 상한을 넘으면 유행어 없는 고정 문장을 쓴다`() {
        val slangy = evidenceOf(brief).mapIndexed { index, card -> if (index < SLANG_CARDS) card + ("line" to "판은 늘 여기서 열렸다, 딸깍.") else card }
        val sheet = RoastSheet.EMPTY.absorb(review(brief, validJson(brief, verdictLine = "숫자 7 넣은 줄.", evidence = slangy)), rules)

        val content = sheet.toContent(brief, fallback, rules)

        assertEquals(fallback.bandOf(GiftGrade.of(TestRoasts.GIFT_SCORE)).plainVerdictLine, content.verdictLine.text)
    }

    @Test
    fun `본인 줄과 칩이 없으면 슬롯이 없고 근거 카드 없이 채운다`() {
        val empty = TestRoasts.brief(emptyList())

        assertEquals(RoastSlots.NONE, empty.slots)
        val content = RoastSheet.EMPTY.toContent(empty, fallback, rules)
        assertEquals(0, content.evidence.size())
        assertNull(content.conclusion?.heroChip)
    }

    companion object {
        /** 덱 상한(세 개)을 채우는 유행어 든 근거 카드 수. */
        private const val SLANG_CARDS = 3
    }
}
