package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRecapFacts
import com.santacut.TestRecapFacts.COAL
import com.santacut.TestRecapFacts.PUSHER_LINES
import com.santacut.TestRecapFacts.ROASTER_LINES
import com.santacut.TestRecapFacts.briefOf
import com.santacut.TestRecapFacts.metric
import com.santacut.TestRecapFacts.phrase
import com.santacut.TestRecapFacts.pusherFacts
import com.santacut.TestRecapFacts.roasterFacts
import com.santacut.TestRecapFacts.thanks
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.ScoreComponents
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 리캡 5차 심사 지적 1·3: 석탄 덱 깎인 말이 모두 말풍선 없는 지표 칩으로 차던 문제와 '고마워' 0번 쏠림.
 * 4차 실측에서 석탄 덱 깎인 말 아홉 장이 모두 quote 없이 나왔고, pusher 덱은 '빨리' 84번을, roaster 덱은 '뭐임'을 잃었다.
 * 원인: 입버릇은 시드 뽑기로 둘만 실려 '빨리'가 빠질 수 있었고, 깎인 말은 line_id 없이도 통과해 작가가 말풍선 없는 지표 칩만 골랐다.
 * 회귀 예문은 합성 대화(pusher·roaster)의 본인 말을 옮긴 것이다.
 */
class RecapQuoteCutTest {
    private val catalog = TestRoasts.catalog
    private val rules = catalog.rules
    private val pusher = briefOf(COAL, pusherFacts(), PUSHER_LINES)
    private val roaster = briefOf(COAL, roasterFacts(), ROASTER_LINES)

    @Test
    fun `깎인 말 입버릇 칩은 나쁜 쪽을 먼저, 그 입버릇이 든 본인 줄이 있는 칩으로 정한다`() {
        assertEquals(PhraseFactSpec.KEY_PREFIX + "ppalri", pusher.quoteCutKey())
        assertEquals(PhraseFactSpec.KEY_PREFIX + "ppalri", pusher.input().quoteCutChipKey)
        assertEquals(PhraseFactSpec.KEY_PREFIX + "mwoim", roaster.quoteCutKey())
        assertEquals(null, briefOf(COAL, pusherFacts(), listOf("아직도 안 옴?", "읽었으면 말을 해")).quoteCutKey())
        assertEquals(null, briefOf(COAL, pusherFacts(), PUSHER_LINES, RoastSlots(2, 0, 0)).quoteCutKey())
    }

    @Test
    fun `회귀 - 깎인 말 한 자리뿐인 선물 덱은 그 자리를 본인 말 입버릇 몫으로 남겨 줄 없는 다른 나쁜 입버릇을 대표 사실로 정하지 않는다`() {
        val facts = listOf(phrase("gwichana", "귀찮"), phrase("ppalri", "빨리"))
        val oneCut = briefOf(GIFT, facts, PUSHER_LINES, RoastSlots(2, 1, 0))
        val twoCuts = briefOf(GIFT, facts, PUSHER_LINES, RoastSlots(2, 2, 0))

        assertEquals(PhraseFactSpec.KEY_PREFIX + "ppalri", oneCut.quoteCutKey())
        assertEquals(null, oneCut.signatureKey())
        assertEquals(PhraseFactSpec.KEY_PREFIX + "gwichana", twoCuts.signatureKey())
        assertEquals(PhraseFactSpec.KEY_PREFIX + "ppalri", briefOf(GIFT, facts.reversed(), PUSHER_LINES, RoastSlots(2, 1, 0)).signatureKey())
    }

    @Test
    fun `첫 응답의 근거 개수 검사가 본인 말 입버릇 깎인 말이 없으면 그 사유를 알린다`() {
        val quoteless = EvidenceDrafts(
            listOf(
                EvidenceDraft("cut", null, "night_ratio", "새벽재촉상", "새벽마다 방을 찌른다."),
                EvidenceDraft("cut", null, PhraseFactSpec.KEY_PREFIX + "haeteo_q", "숙제검사상", "'했어?'로 숙제 검사를 한다."),
                EvidenceDraft("cut", null, ThanksFactSpec.KEY, "감사실종상", "고맙다는 말이 실종됐다."),
                EvidenceDraft("keep", null, "message_count", "출석생존상", "말수 출석은 봐준다."),
            ),
        )
        val quoted = EvidenceDrafts(
            listOf(
                EvidenceDraft("cut", 2, PhraseFactSpec.KEY_PREFIX + "ppalri", "재촉연사상", "'빨리' 소리로 방을 몬다."),
                EvidenceDraft("cut", null, "night_ratio", "새벽재촉상", "새벽마다 방을 찌른다."),
                EvidenceDraft("cut", null, ThanksFactSpec.KEY, "감사실종상", "고맙다는 말이 실종됐다."),
            ),
        )

        assertTrue(quoteless.countCheck(pusher).reasons.contains("cut 한 장은 본인 말풍선을 붙인 입버릇 칩(phrase_ppalri, line_id 필수)으로 쓴다"))
        assertFalse(quoted.countCheck(pusher).reasons.any { reason -> reason.startsWith("cut 한 장은") }, quoted.countCheck(pusher).reasons.toString())
    }

    @Test
    fun `어느 쪽으로도 읽히는 입버릇 뭐임은 깎인 말에 쓸 수 있다`() {
        assertEquals(ChipDirection.EITHER, catalog.phrase.directionOf("mwoim"))
        assertTrue(ChipDirection.EITHER.allows(EvidenceKind.CUT))
        val card = EvidenceDraft("cut", 1, PhraseFactSpec.KEY_PREFIX + "mwoim", "옷평가상", "옷 보자마자 뭐임부터 날린다.")

        assertEquals(emptyList(), card.problems(roaster, rules))
    }

    @Test
    fun `회귀 - 석탄 덱 깎인 말 셋이 모두 말풍선 없는 칩이면 마지막 자리는 받지 않고 다시 쓰기 사유를 남긴다`() {
        val cards = EvidenceDrafts(
            listOf(
                EvidenceDraft("cut", null, "night_ratio", "새벽재촉상", "새벽마다 방을 찌른다."),
                EvidenceDraft("cut", null, PhraseFactSpec.KEY_PREFIX + "haeteo_q", "숙제검사상", "'했어?'로 숙제 검사를 한다."),
                EvidenceDraft("cut", null, ThanksFactSpec.KEY, "감사실종상", "고맙다는 말이 실종됐다."),
            ),
        )

        val intake = EvidenceDrafts.EMPTY.absorbed(cards, passedChecks(cards.size()), pusher, rules)

        assertEquals(2, intake.kept.size())
        assertTrue(intake.reasons.single().contains("본인 말풍선을 붙인 입버릇 칩(phrase_ppalri"), intake.reasons.toString())
        val quoted = EvidenceDrafts(listOf(EvidenceDraft("cut", 2, PhraseFactSpec.KEY_PREFIX + "ppalri", "재촉연사상", "'빨리' 소리로 방을 몬다.")))
        assertEquals(3, intake.kept.absorbed(quoted, passedChecks(1), pusher, rules).kept.size())
    }

    @Test
    fun `본인 말 입버릇 깎인 말이 먼저 들어오면 나머지 깎인 말은 말풍선 없이도 받는다`() {
        val cards = EvidenceDrafts(
            listOf(
                EvidenceDraft("cut", 2, PhraseFactSpec.KEY_PREFIX + "ppalri", "재촉연사상", "'빨리' 소리로 방을 몬다."),
                EvidenceDraft("cut", null, "night_ratio", "새벽재촉상", "새벽마다 방을 찌른다."),
                EvidenceDraft("cut", null, ThanksFactSpec.KEY, "감사실종상", "고맙다는 말이 실종됐다."),
            ),
        )

        val intake = EvidenceDrafts.EMPTY.absorbed(cards, passedChecks(cards.size()), pusher, rules)

        assertEquals(3, intake.kept.size())
        assertEquals(emptyList(), intake.reasons)
    }

    @Test
    fun `회귀 - 작가가 끝내 못 채운 석탄 덱도 대체 카드에 빨리 대답좀을 띄운 입버릇 깎인 말이 한 장 있다`() {
        val cards = pusher.fallbackEvidence(catalog.fallback).toList()
        val quoted = cards.filter(RoastEvidence::isQuotedPhraseCut)

        assertEquals(listOf(PhraseFactSpec.KEY_PREFIX + "ppalri"), quoted.map(RoastEvidence::chipKey))
        assertEquals("빨리 대답좀", quoted.single().quote)
        assertEquals(TestRecapFacts.COAL_SLOTS.cut, cards.count { card -> card.kind == EvidenceKind.CUT })
        assertEquals("그 옷 뭐임 ㅋㅋㅋ", roaster.fallbackEvidence(catalog.fallback).toList().single(RoastEvidence::isQuotedPhraseCut).quote)
    }

    @Test
    fun `회귀 - 시드 뽑기가 빨리를 빼던 pusher 대화도 시드마다 빨리 칩과 본인 말 깎인 말 자리가 있다`() {
        val messages = TestDossiers.selves(List(PUSHER_MESSAGES) { index -> PUSHER_SELF[index % PUSHER_SELF.size] }, minutesApart = 7)
        (0 until SEEDS).forEach { seed ->
            val brief = TestRoasts.brief(messages, COAL, COAL_DIMENSIONS, seed)

            assertTrue(brief.hasFact(PhraseFactSpec.KEY_PREFIX + "ppalri"), "시드 $seed ${brief.facts.keys()}")
            assertEquals(PhraseFactSpec.KEY_PREFIX + "ppalri", brief.quoteCutKey(), "시드 $seed")
        }
    }

    @Test
    fun `가장 튀는 입버릇과 깎인 말에 쓸 가장 튀는 입버릇은 반드시 싣는다`() {
        val pool = RoastFacts(listOf(phrase("wa", "와"), phrase("haeteo_q", "했어?"), phrase("ppalri", "빨리"), phrase("mwohae", "뭐해")))

        assertEquals(listOf("phrase_wa", "phrase_ppalri"), pool.requiredPhrases().keys())
        assertEquals(listOf("phrase_haeteo_q"), RoastFacts(listOf(phrase("haeteo_q", "했어?"), phrase("mwohae", "뭐해"))).requiredPhrases().keys())
    }

    @Test
    fun `고마워 0번은 다른 깎인 말 칩이 모자랄 때만 칩 후보에 남는다`() {
        val plenty = RoastFacts(listOf(metric("night_ratio", 0.41, 99.0), phrase("ppalri", "빨리"), phrase("mwoim", "뭐임"), thanks()))
        val scarce = RoastFacts(listOf(metric("night_ratio", 0.41, 99.0), thanks()))

        assertFalse(plenty.withoutSpareThanks(TestRecapFacts.COAL_SLOTS, LINES).keys().contains(ThanksFactSpec.KEY))
        assertTrue(scarce.withoutSpareThanks(TestRecapFacts.COAL_SLOTS, LINES).keys().contains(ThanksFactSpec.KEY))
    }

    @Test
    fun `회귀 - 고마워 0번은 대체 카드에서도 구체적인 깎인 말 칩 뒤에 온다`() {
        val cuts = pusher.fallbackEvidence(catalog.fallback).toList().filter { card -> card.kind == EvidenceKind.CUT }.map(RoastEvidence::chipKey)

        assertEquals(PhraseFactSpec.KEY_PREFIX + "ppalri", cuts.first())
        assertFalse(ThanksFactSpec.KEY in cuts, cuts.toString())
    }

    @Test
    fun `회귀 - 고마워 칩을 뺀 덱도 판정 한 줄을 고맙다는 말로 이끌지 못한다`() {
        val brief = TestRoasts.brief(TestDossiers.selves(List(PUSHER_MESSAGES) { index -> PUSHER_SELF[index % PUSHER_SELF.size] }, minutesApart = 7), COAL, COAL_DIMENSIONS)

        assertFalse(brief.hasFact(ThanksFactSpec.KEY), brief.facts.keys().toString())
        assertNotNull(brief.thanksWordIn("감사 실종 혐의로 체포하겠어."))
    }

    private fun passedChecks(count: Int): RoastChecks = RoastChecks((0 until count).map { index -> RoastCheck(EvidenceDrafts.fieldOf(index), emptyList()) })

    companion object {
        private const val LINES = 5
        private const val GIFT = 7373
        private const val SEEDS = 6
        private const val PUSHER_MESSAGES = 500
        private val COAL_DIMENSIONS = ScoreComponents.fromColumn("[40,40,40,70]")

        /** 합성 pusher 페르소나의 본인 말(가짜 대화 생성기와 같은 문장). */
        private val PUSHER_SELF = listOf(
            "빨리 대답좀", "왜 아무도 답 안 함", "읽었으면 말을 해", "아 진짜 답답하네", "그래서 언제 됨?", "늦지 마라", "아직도 안 옴?",
            "나만 진심이냐", "대답 좀 하라고", "ㅇㅇ", "몰라 알아서 해", "또 늦냐", "야 지금 몇 시냐", "이거 누가 했냐", "아 됐고",
            "빨리빨리", "다들 뭐함", "내일 아홉 시 칼같이",
        )
    }
}
