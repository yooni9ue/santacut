package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 저장한 팩폭 결과를 덱 내용으로 바꾸기. 옛 결과(작가 프롬프트 1~4판)와 근거가 모자란 결과도 다섯 장 이상의 덱이 된다. */
class RoastContentTest {
    private val mapper = TestRoasts.mapper
    private val fallback = TestRoasts.catalog.fallback

    @Test
    fun `4판 결과는 모르는 필드(verdict_mild 등)를 넘겨 읽고 짝의 칩으로 통계 카드와 구간별 대체 결론을 만든다`() {
        val legacy = mapper.readValue(LEGACY_V4_JSON, RoastContent::class.java)

        val deck = legacy.forDeck(GiftVerdict(GIFT), STRONG, fallback)

        assertFalse(legacy.isCurrent())
        assertEquals("옛 판결 한 줄이다.", deck.verdictLine.text)
        val first = deck.evidence.toList().first()
        assertEquals(EvidenceKind.REASON, first.kind)
        assertEquals(RoastChip("ㅋ 한 번에 최대", "12개"), first.chip)
        assertNull(first.quote)
        assertEquals(EvidenceKind.CUT, deck.evidence.toList().last().kind)
        assertTrue(deck.evidence.size() >= RoastEvidences.MIN_CARDS)
        assertEquals(fallback.bandOf(GiftVerdict(GIFT).grade()).title, deck.conclusion?.title?.text)
        assertFalse(mapper.writeValueAsString(deck).contains("pairs"))
    }

    @Test
    fun `옛 석탄 결과는 깎인 말 통계 카드와 살아남은 말 하나를 갖는다`() {
        val legacy = mapper.readValue(LEGACY_V4_JSON, RoastContent::class.java)

        val deck = legacy.forDeck(GiftVerdict(COAL), WEAK, fallback)

        assertEquals(1, deck.evidence.countOf(EvidenceKind.KEEP))
        assertTrue(deck.evidence.countOf(EvidenceKind.CUT) >= 1)
        assertEquals(0, deck.evidence.countOf(EvidenceKind.REASON))
        assertEquals(EvidenceKind.KEEP, deck.evidence.toList().last().kind)
    }

    @Test
    fun `근거가 없는 저활동 선물 결과는 가장 높은 항목 이유와 가장 낮은 항목 깎인 말로 최소 장수를 맞춘다`() {
        val empty = RoastContent.fallbackOf(GiftVerdict(GIFT), fallback)

        val deck = empty.forDeck(GiftVerdict(GIFT), STRONG, fallback)

        assertEquals(listOf(EvidenceKind.REASON, EvidenceKind.CUT), deck.evidence.toList().map(RoastEvidence::kind))
        assertTrue(deck.evidence.toList().all { card -> card.chipKey.startsWith(RoastEvidences.DIMENSION_KEY_PREFIX) && card.quote == null })
        assertEquals(listOf("공감 점수", "협력 점수"), deck.evidence.toList().map { card -> card.chip?.label })
        assertNotNull(deck.conclusion)
    }

    @Test
    fun `모든 항목이 컷 이상인 선물 덱도 이유만으로 끝나지 않고 깎인 말 한 장을 싣는다`() {
        val reasons = List(3) { index -> evidence(EvidenceKind.REASON, "r$index") }
        val content = RoastContent.fallbackOf(GiftVerdict(GIFT), fallback).copy(evidence = RoastEvidences(reasons))

        val deck = content.forDeck(GiftVerdict(GIFT), ScoreComponents.fromColumn("[90,85,80,70]"), fallback)

        assertEquals(List(3) { EvidenceKind.REASON } + EvidenceKind.CUT, deck.evidence.toList().map(RoastEvidence::kind))
        assertEquals("협력 점수", deck.evidence.toList().last().chip?.label)
    }

    @Test
    fun `종류별 상한을 넘은 근거는 자르고 덱 순서로 줄 세운다`() {
        val cards = List(5) { index -> evidence(EvidenceKind.REASON, "r$index") } + List(3) { index -> evidence(EvidenceKind.CUT, "c$index") }
        val content = RoastContent.fallbackOf(GiftVerdict(GIFT), fallback).copy(evidence = RoastEvidences(cards.reversed()))

        val deck = content.forDeck(GiftVerdict(GIFT), STRONG, fallback)

        assertEquals(List(3) { EvidenceKind.REASON } + List(2) { EvidenceKind.CUT }, deck.evidence.toList().map(RoastEvidence::kind))
    }

    @Test
    fun `지금 형식 결과는 작가 문장이 있을 때만 재사용 대상이다`() {
        val fallbackOnly = RoastContent.fallbackOf(GiftVerdict(GIFT), fallback)
        val withAi = fallbackOnly.copy(verdictLine = RoastText("작가 한 줄이다.", RoastSource.AI))

        assertFalse(fallbackOnly.hasAiText())
        assertTrue(withAi.hasAiText())
        assertFalse(mapper.readValue(LEGACY_V4_JSON, RoastContent::class.java).hasAiText())
    }

    private fun evidence(kind: EvidenceKind, key: String) =
        RoastEvidence(kind, null, key, RoastChip("칩", "1번"), null, "산타인정상", "산타도 인정한다.", RoastSource.AI)

    companion object {
        private const val GIFT = 7373
        private const val COAL = 4200
        private val STRONG = ScoreComponents.fromColumn("[90,80,70,66]")
        private val WEAK = ScoreComponents.fromColumn("[40,50,60,70]")

        /** 작가 프롬프트 4판 결과 꼴(합성 예시). */
        private val LEGACY_V4_JSON = """
            {"verdict":"gift","title":{"text":"옛 칭호","source":"AI"},
            "verdict_line":{"text":"옛 판결 한 줄이다.","source":"AI"},
            "verdict_mild":{"text":"옛 담백 판결이다.","source":"AI"},
            "pairs":[{"fact_key":"laugh_max_run","roast":"옛 놀림","flip":"근데 옛 뒤집기","source":"AI","chip":{"label":"ㅋ 한 번에 최대","value":"12개"}},
            {"fact_key":"question_ratio","roast":"옛 놀림","flip":"근데 옛 뒤집기","source":"AI","chip":{"label":"물음표 살포율","value":"34%"}},
            {"fact_key":"generic","roast":"일반 놀림","flip":"근데 일반 뒤집기","source":"FALLBACK"}],
            "letter":{"text":"옛 쪽지","source":"AI"},
            "story":[{"act":1,"line":"봄. 옛 줄거리.","source":"AI"}]}
        """.trimIndent()
    }
}
