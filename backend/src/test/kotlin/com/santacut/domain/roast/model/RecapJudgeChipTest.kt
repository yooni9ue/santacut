package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.dossier.model.DossierMetric
import com.santacut.domain.dossier.model.MetricReading
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 리캡 근거 카드의 칩 방향·말풍선 짝·대체 통계 카드·칩 글자(심사 지적 C, D, E, F, G, H). 칩은 방향이 맞는 종류에만 쓰고,
 * 시간대 칩은 말풍선 없이 쓰며, 뜻 없는 값이나 중립 칩은 통계 카드에도 쓰지 않는다. 회귀 예문은 심사 덱의 문장을 합성 입력에 옮긴 것이다.
 */
class RecapJudgeChipTest {
    private val catalog = TestRoasts.catalog
    private val rules = catalog.rules

    @Test
    fun `모든 지표 칩은 수준마다 방향을 갖고 시간대·요일·묶음 칩은 말풍선 없이 쓴다`() {
        assertEquals(emptyList(), catalog.violations())
        catalog.facts.toList().forEach { spec -> assertEquals(spec.levels.size, spec.directions.size, spec.key) }
        listOf("night_ratio", "morning_ratio", "office_hours_ratio", "weekend_ratio", "peak_hour", "peak_hour_share", "burst_avg", "burst_max")
            .forEach { key -> assertEquals(QuoteRule.NONE, catalog.facts.find(key)?.quoteRule, key) }
        val timed = assertNotNull(catalog.facts.find("night_ratio")).copy(quoteRule = QuoteRule.NIGHT)
        assertTrue(timed.violations().any { reason -> reason.contains("센 글자가 보이는 것") })
    }

    @Test
    fun `회귀 - 맛집 리스트 공유함에 피크타임 몰빵률 칩은 말풍선을 붙일 수 없고 칩에 없는 점심을 말하면 떨어진다`() {
        val brief = briefOf(GIFT, metric("peak_hour_share", 0.15, VERY_HIGH), quotes = listOf(LineQuote(SHARE, "맛집 리스트 공유함", MONDAY.withHour(12))))

        val withQuote = EvidenceDraft("cut", SHARE, "peak_hour_share", "저녁집중상", "피크타임 공유 폭격이 매섭다.").problems(brief, rules)
        val inventedTime = TextProblems.timeClaims("점심마다 공유 폭격이 매섭다.", rules, brief.chipTimeTextsOf("peak_hour_share"), null).toList()

        assertTrue(withQuote.contains("이 칩(peak_hour_share)은 말풍선 없이 쓴다(line_id를 null로)"), withQuote.toString())
        assertTrue(withQuote.any { reason -> reason.startsWith("이 칩(peak_hour_share)은 cut에 쓰지 않는다") }, withQuote.toString())
        assertEquals(listOf("칩이 말하지 않는 시간대(점심)"), inventedTime)
        assertEquals(emptyList(), TextProblems.timeClaims("피크타임에 방을 다 쓴다.", rules, brief.chipTimeTextsOf("peak_hour_share"), null).toList())
        assertEquals(emptyList(), TextProblems.timeClaims("점심 약속을 또 던진다.", rules, emptyList(), "점심 같이 먹을 사람").toList())
    }

    @Test
    fun `회귀 - 하루 폭주 배수, 달별 기복, 근무시간 출몰률, 평균 길이 같은 중립 칩은 어느 근거에도 쓰지 못한다`() {
        val neutral = listOf(
            metric("busiest_day_multiple", 2.8, VERY_LOW) to "폭주 없이 고르게 말을 건다.",
            metric("month_ratio", 2.0, VERY_LOW) to "기복이 롤러코스터급이다.",
            metric("office_hours_ratio", 0.1, VERY_LOW) to "근무시간 방 잠적이 완벽하다.",
            metric("avg_length", 6.9, VERY_LOW) to "한마디씩 툭툭 던진다.",
        )
        val brief = briefOf(COAL, *neutral.map { pair -> pair.first }.toTypedArray())

        neutral.forEach { (fact, line) ->
            assertEquals(ChipDirection.NEUTRAL, fact.direction, fact.key)
            listOf("reason", "cut", "keep").forEach { kind ->
                val reasons = EvidenceDraft(kind, null, fact.key, "기복과속상", line).problems(brief, rules)
                assertTrue(reasons.any { reason -> reason.startsWith("이 칩(${fact.key})은 ${kind}에 쓰지 않는다") }, "$kind $reasons")
            }
        }
        assertEquals(emptyList<String>(), brief.input().chips.toList().first { hint -> hint.chipKey == "month_ratio" }.kinds)
    }

    @Test
    fun `회귀 - 칭찬 입버릇 와는 깎인 말 근거가 될 수 없고 나쁜 입버릇은 선물 받는 이유가 될 수 없다`() {
        val wa = phrase("wa", "와")
        val ppalli = phrase("ppalri", "빨리")
        val brief = briefOf(COAL, wa, ppalli, quotes = listOf(LineQuote(WOW, "와 센스 무엇"), LineQuote(RUSH, "빨리 대답 좀 해")))

        assertEquals(ChipDirection.GOOD, wa.direction)
        assertEquals(ChipDirection.BAD, ppalli.direction)
        assertEquals(ChipDirection.EITHER, catalog.phrase.directionOf("chulbal"))
        assertTrue(EvidenceDraft("cut", WOW, wa.key, "감탄연발상", "와 소리로 방을 흔든다.").problems(brief, rules).any { reason ->
            reason.startsWith("이 칩(${wa.key})은 cut에 쓰지 않는다")
        })
        assertTrue(EvidenceDraft("keep", RUSH, ppalli.key, "재촉생존상", "빨리 소리로 판을 굴린다.").problems(brief, rules).any { reason ->
            reason.startsWith("이 칩(${ppalli.key})은 keep에 쓰지 않는다")
        })
    }

    @Test
    fun `회귀 - 수고했다 근데 별로임은 칭찬을 뒤집는 말풍선이라 살아남은 말 근거가 될 수 없다`() {
        val kind = metric("kind_words_density", 18.0, VERY_HIGH)
        val brief = briefOf(COAL, kind, quotes = listOf(LineQuote(FLIP, "수고했다 근데 별로임")), kinds = true)

        val reasons = EvidenceDraft("keep", FLIP, kind.key, "수고배달상", "수고 소리는 꼬박 챙긴다.").problems(brief, rules)

        assertTrue(reasons.contains("칭찬을 뒤집는 말풍선(근데)은 reason·keep 근거로 쓰지 않는다"), reasons.toString())
        assertFalse(EvidenceDraft("cut", FLIP, kind.key, "수고배달상", "수고 소리는 꼬박 챙긴다.").problems(brief, rules).any { reason ->
            reason.startsWith("칭찬을 뒤집는 말풍선")
        })
    }

    @Test
    fun `회귀 - 지표 칩 이름을 그대로 되읽는 한 줄은 떨어지고 버릇 낱말로 말하면 통과한다`() {
        val short = metric("short_ratio", 0.6, VERY_HIGH)
        val brief = briefOf(COAL, short)

        assertTrue(EvidenceDraft("cut", null, short.key, "단답끊기상", "짧은 말풍선만 골라 보낸다.").problems(brief, rules).contains("line: 칩 이름을 그대로 옮겼다(칩이 센 버릇의 뜻을 말한다)"))
        assertEquals(emptyList(), EvidenceDraft("cut", null, short.key, "단답끊기상", "단답으로 대화를 툭 끊는다.").problems(brief, rules))
    }

    @Test
    fun `일반론 검사는 칩 글자가 없어도 칩이 센 버릇의 낱말과 겹치면 통과한다`() {
        val chipTexts = listOf("짧은 말풍선", "다섯 글자 이하로 끝난 말풍선의 비율", "단답", "짧은")

        assertEquals(emptyList(), TextProblems.grounding("단답으로 대화를 툭 끊는다.", null, chipTexts).toList())
        assertFalse(TextProblems.grounding("방 분위기를 확 바꿨다.", null, chipTexts).toList().isEmpty())
    }

    @Test
    fun `회귀 - 연달아 보내기 한 개처럼 뜻 없는 값은 사실이 되지 않고 대체 통계 카드는 방향이 맞는 칩만 쓴다`() {
        assertNull(metricOrNull("burst_avg", 1.2, VERY_LOW))
        assertNull(metricOrNull("burst_avg", 2.9, VERY_HIGH))
        assertNotNull(metricOrNull("burst_avg", 3.4, VERY_HIGH))
        val facts = RoastFacts(listOf(metric("month_ratio", 20.0, VERY_HIGH), metric("night_ratio", 0.3, VERY_HIGH), metric("question_ratio", 0.3, VERY_HIGH)))

        val cards = facts.chipFacts().fallbackEvidence(RoastSlots(0, 2, 1), catalog.fallback)

        assertEquals(listOf("night_ratio", "question_ratio"), cards.toList().map(RoastEvidence::chipKey))
        assertEquals(listOf(EvidenceKind.CUT, EvidenceKind.KEEP), cards.toList().map(RoastEvidence::kind))
    }

    @Test
    fun `회귀 - 새벽 출몰이 가장 튀는 사람은 그 칩이 대표 사실이고 근거나 대표 칩에 쓰지 않으면 결론이 떨어진다`() {
        val night = metric("night_ratio", 0.4, VERY_HIGH)
        val month = metric("month_ratio", 30.0, VERY_HIGH)
        val facts = RoastFacts(listOf(month.copy(score = 1.0), night)).drawn(Random(0), RoastFacts.MAX_DRAWN_FACTS)
        val brief = briefOf(COAL, *facts.toList().toTypedArray(), thanks())

        assertEquals("night_ratio", facts.signature()?.key)
        assertEquals("night_ratio", facts.toList().first().key)
        assertEquals("night_ratio", brief.input().signatureChipKey)
        val missing = ConclusionDraft("새벽침묵러", null, "month_ratio", "새벽 기복 결산. 넌 나가라.").check(brief, rules)
        assertTrue(missing.reasons.contains("대표 사실 칩(night_ratio)을 근거 카드나 hero_chip_key 가운데 한 곳에 쓴다"), missing.reasons.toString())
    }

    @Test
    fun `회귀 - 고마워 0번은 다른 나쁜 쪽 칩 뒤에 서서 깎인 말 한 장까지만 채운다`() {
        val facts = RoastFacts(listOf(metric("night_ratio", 0.4, VERY_HIGH), metric("short_ratio", 0.6, VERY_HIGH), thanks()))

        val cards = facts.chipFacts().fallbackEvidence(RoastSlots(0, 2, 0), catalog.fallback)
        val all = facts.chipFacts().fallbackEvidence(RoastSlots(0, 3, 0), catalog.fallback)

        assertEquals(listOf("night_ratio", "short_ratio"), cards.toList().map(RoastEvidence::chipKey))
        assertEquals(1, all.toList().count { evidence -> evidence.chipKey == ThanksFactSpec.KEY })
        assertNull(facts.signature()?.takeIf { fact -> fact.key == ThanksFactSpec.KEY })
    }

    @Test
    fun `회귀 - 입버릇 칩 글자는 발사 횟수 대신 따옴표 입버릇과 횟수만 쓴다`() {
        assertEquals("'{quote}'", catalog.phrase.chipLabel)
        assertFalse(catalog.phrase.chipLabel.contains("발사"))
        assertEquals("'출발' 43번", phrase("chulbal", "출발").chip.text())
    }

    @Test
    fun `회귀 - 따뜻한 말 100개당 26점6개 칩은 4번에 1번처럼 읽히게 찍는다`() {
        assertEquals("4번에 1번", metric("kind_words_density", 26.6, VERY_HIGH).chip.value)
        assertEquals("한 번에 2개", metric("laugh_density", 200.0, VERY_HIGH).chip.value)
        assertEquals("15%", metric("peak_hour_share", 0.15, VERY_HIGH).chip.value)
    }

    @Test
    fun `칩 방향에 따라 근거 카드 영역 태그용 항목을 싣는다`() {
        assertEquals("cooperation_sharing", metric("question_ratio", 0.3, VERY_HIGH).dimension)
        assertEquals("respectful_communication", thanks().dimension)
        val brief = briefOf(GIFT, metric("question_ratio", 0.3, VERY_HIGH), quotes = listOf(LineQuote(ASK, "내일 저녁 가능??")))
        val evidence = assertNotNull(EvidenceDraft("reason", ASK, "question_ratio", "물음표총대상", "물음표로 약속을 굴린다.").toEvidence(brief))
        assertEquals("cooperation_sharing", evidence.dimension)
    }

    private fun metric(key: String, value: Double, percentile: Double): RoastFact = assertNotNull(metricOrNull(key, value, percentile), key)

    private fun metricOrNull(key: String, value: Double, percentile: Double): RoastFact? {
        val spec = assertNotNull(catalog.facts.find(key))
        val metricSpec = TestRoasts.dossier.metrics.toList().first { candidate -> candidate.key == key }
        return spec.factOf(MetricReading(assertNotNull(DossierMetric.ofKey(key)), metricSpec, value, percentile))
    }

    private fun phrase(key: String, display: String): RoastFact = RoastFact(
        PhraseFactSpec.KEY_PREFIX + key, "'$display'라고 보낸 횟수", "남들보다 많음", "habit", RoastChip(catalog.phrase.chipLabel.replace(PhraseFactSpec.QUOTE, display), "43번"),
        0.9, direction = catalog.phrase.directionOf(key),
    )

    private fun thanks(): RoastFact = RoastFact(
        ThanksFactSpec.KEY, catalog.thanks.meaning, catalog.thanks.level, catalog.thanks.group, RoastChip("'고마워'", "0번"),
        direction = ChipDirection.BAD, dimension = catalog.thanks.dimension,
    )

    private fun briefOf(hundredths: Int, vararg facts: RoastFact, quotes: List<LineQuote> = emptyList(), kinds: Boolean = false): RoastBrief {
        val verdict = GiftVerdict(hundredths)
        return RoastBrief(
            verdict, RoastFacts(facts.toList()), RoastQuotes.EMPTY, RoastTopics.EMPTY, RoastLines.EMPTY, BlockedWords.EMPTY,
            slang = rules.slang.allowed, lineQuotes = LineQuotes(quotes), phrases = TestRoasts.dossier.phrases,
            kinds = TestRoasts.dossier.kinds.takeIf { kinds }, foreignMarks = catalog.punch.foreignMarksFor(verdict.grade()), examples = catalog.examples,
            thanksWords = catalog.thanks.words,
        )
    }

    companion object {
        private const val GIFT = 7373
        private const val COAL = 4915
        private const val VERY_HIGH = 95.0
        private const val VERY_LOW = 5.0
        private const val SHARE = 1
        private const val WOW = 2
        private const val RUSH = 3
        private const val FLIP = 4
        private const val ASK = 5
        private val MONDAY: LocalDateTime = LocalDateTime.of(2026, 3, 2, 10, 0)
    }
}
