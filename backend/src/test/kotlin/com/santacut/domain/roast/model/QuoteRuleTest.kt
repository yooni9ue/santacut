package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.dossier.model.DossierMetric
import com.santacut.domain.dossier.model.MetricReading
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 지표 칩과 말풍선이 같은 버릇을 가리키는지 보는 말풍선 조건. 말풍선으로 못 보여 주는 칩은 말풍선 없이 통계 카드로 쓴다. */
class QuoteRuleTest {
    private val rules = TestRoasts.catalog.rules
    private val kinds = TestRoasts.dossier.kinds

    @Test
    fun `글자 조건은 카드에 뜨는 말풍선 표시로 본다`() {
        assertTrue(QuoteRule.QUESTION.accepts(quote("내일 저녁 가능?"), null, kinds))
        assertFalse(QuoteRule.QUESTION.accepts(quote("내일 저녁 가능"), null, kinds))
        assertTrue(QuoteRule.EXCLAIM.accepts(quote("드디어 끝났다!"), null, kinds))
        assertFalse(QuoteRule.EXCLAIM.accepts(quote("드디어 끝났다"), null, kinds))
        assertTrue(QuoteRule.LAUGH.accepts(quote("ㅋㅋㅋ 이거 뭐야"), null, kinds))
        assertFalse(QuoteRule.LAUGH.accepts(quote("이거 뭐야 진짜"), null, kinds))
        assertTrue(QuoteRule.KIND.accepts(quote("오늘 얘기 들어줘서 고마워"), null, kinds))
        assertFalse(QuoteRule.KIND.accepts(quote("카페 여기 어때"), null, kinds))
    }

    @Test
    fun `시각 조건은 보낸 시각과 요일로 보고 시각을 모르면 맞지 않는다`() {
        assertTrue(QuoteRule.NIGHT.accepts(quote("라면 끓일 사람", at(2)), null, kinds))
        assertFalse(QuoteRule.NIGHT.accepts(quote("라면 끓일 사람", at(5)), null, kinds))
        assertTrue(QuoteRule.MORNING.accepts(quote("다들 일어났냐", at(7)), null, kinds))
        assertFalse(QuoteRule.MORNING.accepts(quote("다들 일어났냐", at(9)), null, kinds))
        assertTrue(QuoteRule.OFFICE.accepts(quote("회의 끝나고 볼까", at(17)), null, kinds))
        assertFalse(QuoteRule.OFFICE.accepts(quote("회의 끝나고 볼까", at(18)), null, kinds))
        assertTrue(QuoteRule.PEAK_HOUR.accepts(quote("저녁 뭐 먹을까", at(21)), 21, kinds))
        assertFalse(QuoteRule.PEAK_HOUR.accepts(quote("저녁 뭐 먹을까", at(20)), 21, kinds))
        assertFalse(QuoteRule.PEAK_HOUR.accepts(quote("저녁 뭐 먹을까", at(21)), null, kinds))
        assertTrue(QuoteRule.WEEKEND.accepts(quote("오늘 어디 갈까", SATURDAY), null, kinds))
        assertFalse(QuoteRule.WEEKEND.accepts(quote("오늘 어디 갈까", MONDAY), null, kinds))
        assertFalse(QuoteRule.NIGHT.accepts(LineQuote(1, "라면 끓일 사람"), null, kinds))
    }

    @Test
    fun `연달아 보내기 조건은 묶음 안의 줄만 받고 말풍선 없는 칩 조건은 어떤 줄도 받지 않는다`() {
        assertTrue(QuoteRule.BURST.accepts(LineQuote(1, "빨리 대답좀", MONDAY, burst = true), null, kinds))
        assertFalse(QuoteRule.BURST.accepts(LineQuote(1, "빨리 대답좀", MONDAY, burst = false), null, kinds))
        assertFalse(QuoteRule.NONE.accepts(quote("무엇이든 좋은 말"), null, kinds))
    }

    @Test
    fun `facts의 모든 지표는 말풍선 조건을 갖고 고마워 칩은 말풍선 없이, 입버릇 칩은 입버릇 조건으로 쓴다`() {
        val specs = TestRoasts.catalog.facts.toList()
        assertTrue(specs.none { spec -> spec.quoteRule == QuoteRule.PHRASE })
        assertEquals(QuoteRule.QUESTION, TestRoasts.catalog.facts.find("question_ratio")?.quoteRule)
        assertEquals(QuoteRule.NONE, TestRoasts.catalog.facts.find("emoji_density")?.quoteRule)
        assertEquals(QuoteRule.NONE, TestRoasts.catalog.facts.find("media_ratio")?.quoteRule)
        assertEquals(QuoteRule.NONE, RoastFact(ThanksFactSpec.KEY, "뜻", "수준", "thanks", RoastChip("'고마워'", "0번")).quoteRule)
        assertEquals(QuoteRule.PHRASE, RoastFact(PhraseFactSpec.KEY_PREFIX + "ppalli", "뜻", "수준", "habit", RoastChip("'빨리' 발사 횟수", "9번")).quoteRule)
    }

    @Test
    fun `회귀 - 언제 가능에 피크타임 칩, 카페 여기 어때에 따뜻한 말 칩은 맞지 않아 떨어지고 미안 늦게 봤어에 짧은 말풍선 칩은 말풍선 없이 쓰라고 떨어진다`() {
        val brief = briefOf(
            LineQuote(WHEN, "○○는 언제 가능?", at(14)),
            LineQuote(SORRY, "미안 늦게 봤어", at(21)),
            LineQuote(CAFE, "카페 여기 어때", at(21)),
        )

        listOf(
            EvidenceDraft("cut", WHEN, PEAK_SHARE, "시간독촉상", "피크타임마다 같은 질문이 온다."),
            EvidenceDraft("reason", CAFE, KIND_WORDS, "카페추천상", "따뜻한 말로 카페 지도를 폈다."),
        ).forEach { draft ->
            assertTrue(draft.problems(brief, rules).any { reason -> reason.startsWith("말풍선이 칩과 맞지 않는다") }, draft.toString())
        }
        val shortChip = EvidenceDraft("cut", SORRY, SHORT_RATIO, "답장지연상", "짧은 답 대신 사과가 먼저 온다.")
        assertTrue(shortChip.problems(brief, rules).contains("이 칩($SHORT_RATIO)은 말풍선 없이 쓴다(line_id를 null로)"))
        assertEquals(QuoteRule.NONE, TestRoasts.catalog.facts.find(SHORT_RATIO)?.quoteRule)
        val fitting = EvidenceDraft("cut", WHEN, QUESTION, "질문살포상", "물음표 살포로 약속을 굴린다.")
        assertFalse(fitting.problems(brief, rules).any { reason -> reason.contains("말풍선이 칩과") }, fitting.problems(brief, rules).toString())
    }

    @Test
    fun `말풍선으로 못 보여 주는 칩은 어느 종류든 line_id 없이 쓰고 말풍선을 붙이면 떨어진다`() {
        val brief = briefOf(LineQuote(SORRY, "미안 늦게 봤어", at(21)))

        val withQuote = EvidenceDraft("reason", SORRY, EMOJI, "이모지장인상", "이모지로 말풍선을 꾸몄다.")
        val statCard = EvidenceDraft("reason", null, EMOJI, "이모지장인상", "이모지로 말풍선을 꾸몄다.")

        assertTrue(withQuote.problems(brief, rules).contains("이 칩($EMOJI)은 말풍선 없이 쓴다(line_id를 null로)"))
        assertEquals(emptyList(), statCard.problems(brief, rules))
    }

    @Test
    fun `작가 입력의 칩 후보는 말풍선 조건을 싣고 피크타임 조건에는 가장 바쁜 시를 넣는다`() {
        val hints = TestRoasts.mapper.valueToTree<JsonNode>(briefOf().input().chips)
        val byKey = hints.values().associateBy { hint -> hint.path("chip_key").asString() }

        assertEquals(QuoteRule.QUESTION.hint, byKey.getValue(QUESTION).path("quote").asString())
        assertEquals("가장 바쁜 시(21시)에 보낸 말풍선만", byKey.getValue(PEAK_SHARE).path("quote").asString())
        assertEquals(QuoteRule.NONE.hint, byKey.getValue(EMOJI).path("quote").asString())
    }

    @Test
    fun `본인 말풍선끼리 삼 분 안으로 이어 보낸 줄은 연달아 표시를 달고 말풍선 조건에 쓰인다`() {
        val messages = listOf(
            TestDossiers.self(TestDossiers.START, "빨리 대답좀 해줘"),
            TestDossiers.self(TestDossiers.START.plusMinutes(2), "다들 어디 갔냐고"),
            TestDossiers.partner(TestDossiers.START.plusMinutes(3), "왜"),
            TestDossiers.self(TestDossiers.START.plusMinutes(30), "이제야 답장하냐"),
        )
        val brief = TestRoasts.brief(messages)
        val input = brief.inputText(TestRoasts.mapper)

        assertTrue(input.contains("1|03-02 월 10시 연달아|"), input)
        assertTrue(input.contains("2|03-02 월 10시 연달아|"), input)
        assertTrue(input.contains("3|03-02 월 10시|"), input)
    }

    @Test
    fun `낮음 수준 지표 칩은 버릇이 드물다는 칩이라 말풍선 없이 쓴다`() {
        val spec = assertNotNull(TestRoasts.catalog.facts.find("night_ratio"))
        val metric = TestRoasts.dossier.metrics.toList().first { candidate -> candidate.key == "night_ratio" }

        val rare = spec.factOf(MetricReading(DossierMetric.NIGHT_RATIO, metric, RARE_VALUE, VERY_LOW_PERCENTILE))
        val low = spec.factOf(MetricReading(DossierMetric.NIGHT_RATIO, metric, RARE_VALUE, LOW_PERCENTILE))
        val frequent = spec.factOf(MetricReading(DossierMetric.NIGHT_RATIO, metric, FREQUENT_VALUE, HIGH_PERCENTILE))

        assertEquals(QuoteRule.NONE, rare?.quoteRule)
        assertEquals(QuoteRule.NONE, low?.quoteRule)
        assertEquals(QuoteRule.NIGHT, frequent?.quoteRule)
    }

    @Test
    fun `가장 바쁜 시를 모르면 피크타임 칩은 말풍선 없이 쓴다`() {
        val unknownPeak = briefOf(LineQuote(WHEN, "○○는 언제 가능?", at(14)), peakHour = null)

        assertEquals(QuoteRule.NONE, unknownPeak.quoteRuleOf(PEAK_SHARE))
        assertEquals(QuoteRule.PEAK_HOUR, briefOf().quoteRuleOf(PEAK_SHARE))
        assertEquals(emptyList(), EvidenceDraft("cut", null, PEAK_SHARE, "몰빵출몰상", "피크타임에만 몰빵한다.").problems(unknownPeak, rules))
    }

    private fun briefOf(vararg quotes: LineQuote, peakHour: Int? = PEAK_HOUR): RoastBrief = RoastBrief(
        GiftVerdict(TestRoasts.GIFT_SCORE), FACTS, RoastQuotes.EMPTY, RoastTopics.EMPTY, RoastLines.EMPTY, BlockedWords.EMPTY,
        lineQuotes = LineQuotes(quotes.toList()), kinds = kinds, peakHour = peakHour,
    )

    private fun quote(text: String, time: LocalDateTime = MONDAY): LineQuote = LineQuote(1, text, time)

    private fun at(hour: Int): LocalDateTime = MONDAY.withHour(hour)

    companion object {
        private const val WHEN = 1
        private const val SORRY = 2
        private const val CAFE = 3
        private const val PEAK_HOUR = 21
        private const val RARE_VALUE = 0.01
        private const val FREQUENT_VALUE = 0.3
        private const val VERY_LOW_PERCENTILE = 5.0
        private const val LOW_PERCENTILE = 20.0
        private const val HIGH_PERCENTILE = 95.0
        private const val PEAK_SHARE = "peak_hour_share"
        private const val SHORT_RATIO = "short_ratio"
        private const val KIND_WORDS = "kind_words_density"
        private const val QUESTION = "question_ratio"
        private const val EMOJI = "emoji_density"

        /** 2026-03-02(월) 10시. */
        private val MONDAY: LocalDateTime = LocalDateTime.of(2026, 3, 2, 10, 0)
        private val SATURDAY: LocalDateTime = LocalDateTime.of(2026, 3, 7, 10, 0)

        private val FACTS = RoastFacts(
            listOf(
                fact(PEAK_SHARE, "피크타임 몰빵률", "15%", QuoteRule.PEAK_HOUR),
                fact(SHORT_RATIO, "짧은 말풍선", "16%", QuoteRule.NONE),
                fact(KIND_WORDS, "따뜻한 말", "100개당 26.6개", QuoteRule.KIND),
                fact(QUESTION, "물음표 살포율", "34%", QuoteRule.QUESTION),
                fact(EMOJI, "이모지 밀도", "100개당 3.2개", QuoteRule.NONE),
            ),
        )

        private fun fact(key: String, label: String, value: String, rule: QuoteRule): RoastFact =
            RoastFact(key, label, "남들보다 많음", key, RoastChip(label, value), 1.0, rule)
    }
}
