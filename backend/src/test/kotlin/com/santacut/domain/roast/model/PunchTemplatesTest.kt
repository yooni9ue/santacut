package com.santacut.domain.roast.model

import com.santacut.TestRoastAnswers.finalLineFor
import com.santacut.TestRoastAnswers.habitMessages
import com.santacut.TestRoastAnswers.heroOf
import com.santacut.TestRoastAnswers.review
import com.santacut.TestRoastAnswers.validJson
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.analysis.model.GiftVerdict
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 결론 마지막 한 줄의 펀치 틀(roast/punch.json). 참가자·해 시드로 추천 틀을 골라 사람마다 다른 펀치를 쓰게 하고,
 * 판정 한 줄과 같은 유행어·같은 틀을 되풀이하지 않게 한다. 대체 마지막 한 줄도 시드마다 다르다.
 */
class PunchTemplatesTest {
    private val rules = TestRoasts.catalog.rules
    private val punch = TestRoasts.catalog.punch

    @Test
    fun `구간·판정 풀마다 서로 다른 펀치 구조가 여섯 개 이상이고 리소스 위반이 없다`() {
        assertEquals(emptyList(), punch.violations(rules))
        (punch.bands.pools() + punch.skins.pools()).forEach { pool ->
            assertTrue(pool.markerCount() >= RoastPunch.MIN_STRUCTURES, pool.markers().toString())
            assertTrue(pool.hasPlain(rules))
        }
        val markers = (punch.bands.pools() + punch.skins.pools()).flatMap(PunchTemplates::markers).toSet()
        listOf("체포하겠", "L을 가져가", "딸깍", "요이~", "아뇨아뇨아뇨", "줴줴이야", "엄…", "예?", "넌 나가", "넌 남아", "티~원", "퇴사할게여", "프리패스", "야~")
            .forEach { marker -> assertTrue(marker in markers, marker) }
    }

    @Test
    fun `서로 다른 세 시드는 서로 다른 첫 추천 틀을 받고 같은 시드는 늘 같은 추천을 받는다`() {
        listOf(TestRoasts.GIFT_SCORE, TestRoasts.COAL_SCORE).forEach { hundredths ->
            val firsts = SEEDS.map { seed -> punch.candidatesFor(GiftVerdict(hundredths), seed).take(1).hints().single() }
            assertEquals(SEEDS.size, firsts.toSet().size, firsts.toString())
            SEEDS.forEach { seed ->
                assertEquals(punch.candidatesFor(GiftVerdict(hundredths), seed).hints(), punch.candidatesFor(GiftVerdict(hundredths), seed).hints())
            }
        }
    }

    @Test
    fun `작가에게 정해 준 마지막 한 줄 틀 하나를 보낸다`() {
        val brief = TestRoasts.brief(habitMessages(), seed = SEEDS.first())
        val sent = TestRoasts.mapper.readTree(brief.inputText(TestRoasts.mapper)).path("punch_templates").values().map { node -> node.asString() }

        assertEquals(punch.suggestions, sent.size)
        assertEquals(brief.suggestedPunches.hints(), sent)
        assertEquals(sent.size, brief.suggestedPunches.markers().toSet().size)
    }

    @Test
    fun `마지막 한 줄은 추천 틀 가운데 하나를 써야 하고 추천 밖의 펀치는 떨어진다`() {
        val brief = TestRoasts.brief(habitMessages())
        val outside = (punch.poolOf(brief.verdict).markers() - brief.suggestedPunches.markers().toSet()).first()
        val used = finalLineFor(brief, heroOf(brief).second, "")

        val passed = review(brief, validJson(brief, verdictLine = PLAIN_VERDICT, finalLine = used)).checks
        assertTrue(passed.passed(ConclusionDraft.FIELD), passed.reasonsFor(listOf(ConclusionDraft.FIELD)).toString())
        val wrong = TextProblems.of("발사 장부 결산. $outside", RoastTextKind.FINAL_LINE, brief, rules).toList()
        assertTrue(wrong.any { reason -> reason.startsWith("추천 펀치 틀(punch_templates)을 쓰지 않았다") }, wrong.toString())
    }

    @Test
    fun `회귀 - 세 사람 모두 올해 너는 OO야 틀을 쓰면 추천이 다른 사람에게서 떨어진다`() {
        val finals = mapOf("organizer" to "올해 너는 '출발'이야~", "pusher" to "올해 너는 '빨리'야~", "sweet" to "올해 너는 '고마워'야~")
        val rejected = SEEDS.zip(finals.values).count { (seed, line) ->
            val brief = TestRoasts.brief(habitMessages(), seed = seed)
            TextProblems.of(line, RoastTextKind.FINAL_LINE, brief, rules).toList().any { reason -> reason.startsWith("추천 펀치 틀") }
        }

        assertTrue(rejected >= 1, "세 시드 가운데 적어도 한 사람은 '야~' 틀을 추천받지 않는다")
    }

    @Test
    fun `회귀 - 판정 한 줄과 같은 틀·같은 유행어를 마지막 한 줄에 되풀이하면 결론이 떨어진다`() {
        val brief = TestRoasts.brief(habitMessages(), TestRoasts.COAL_SCORE)

        assertTrue(rules.punchClashIn("답장 독촉 혐의로 체포하겠어.", "독촉 상습 혐의로 체포하겠어.") != null)
        assertTrue(rules.punchClashIn("올해 너는 '독촉'이야~", "올해 너는 '빨리'야~") != null)
        assertEquals(null, rules.punchClashIn("선물 상자 탑승 완료, 딸깍.", "착한 아이 확정이다. 넌 남아라."))

        val verdict = "독촉 상습 혐의로 체포하겠어."
        val clash = review(brief, validJson(brief, verdictLine = verdict, finalLine = "발사 장부 혐의로 체포하겠어.")).checks
        assertFalse(clash.passed(ConclusionDraft.FIELD))
        assertTrue(clash.reasonsFor(listOf(ConclusionDraft.FIELD)).any { reason -> reason.contains("판정 한 줄과 같은 펀치") })
    }

    @Test
    fun `다른 응답에서 온 결론도 이미 받은 판정 한 줄과 펀치가 겹치면 받지 않는다`() {
        val brief = TestRoasts.briefWithoutPunches(habitMessages())
        val verdict = "올해 장부 결산, 딸깍."
        val first = review(brief, validJson(brief, verdictLine = verdict, title = "두 배 장인"))
        val sheet = RoastSheet.EMPTY.absorb(first, brief, rules)
        assertEquals(listOf(ConclusionDraft.FIELD), sheet.missingFields())
        val clashingFinal = finalLineFor(brief, heroOf(brief).second, "").replace("요이~", "딸깍.")
        val clashing = validJson(brief, verdictLine = "다른 판정 한 줄이다.", finalLine = clashingFinal)

        val after = sheet.absorb(review(brief, clashing), brief, rules)

        assertEquals(listOf(ConclusionDraft.FIELD), after.missingFields())
    }

    @Test
    fun `대체 마지막 한 줄은 시드마다 다르고 판정 한 줄과 펀치가 겹치지 않는다`() {
        listOf(TestRoasts.GIFT_SCORE, TestRoasts.COAL_SCORE).forEach { hundredths ->
            val contents = SEEDS.map { seed ->
                val brief = TestRoasts.brief(habitMessages(), hundredths, seed = seed)
                RoastSheet.EMPTY.toContent(brief, TestRoasts.catalog.fallback, rules)
            }
            val finals = contents.map { content -> content.conclusion?.finalLine?.text.orEmpty() }

            assertEquals(SEEDS.size, finals.toSet().size, finals.toString())
            contents.forEach { content ->
                assertEquals(null, rules.punchClashIn(content.verdictLine.text, content.conclusion?.finalLine?.text.orEmpty()), content.toString())
                assertEquals(RoastSource.FALLBACK, content.conclusion?.finalLine?.source)
            }
        }
    }

    @Test
    fun `유행어 없는 대체가 필요하면 유행어 없는 펀치 틀 문장을 고른다`() {
        val brief = TestRoasts.brief(habitMessages())
        val verdict = fallbackBand(GiftGrade.of(TestRoasts.GIFT_SCORE)).plainVerdictLine

        val plain = brief.punchFallback(verdict, plain = true, rules = rules).orEmpty()

        assertTrue(rules.isPunch(plain), plain)
        assertEquals(0, rules.slangCountIn(plain))
    }

    @Test
    fun `모든 대체 마지막 한 줄은 검증 규칙을 지키는 펀치이고 자기 틀 표지를 품는다`() {
        GiftGrade.entries.forEach { grade ->
            val verdict = GiftVerdict(HUNDREDTHS.getValue(grade))
            val brief = TestRoasts.briefWithoutPunches(habitMessages(), HUNDREDTHS.getValue(grade))
            punch.poolOf(verdict).toList().forEach { template ->
                assertTrue(template.isUsedIn(template.fallback, rules), template.toString())
                assertEquals(emptyList(), TextProblems.of(template.fallback, RoastTextKind.FINAL_LINE, brief, rules).toList(), "${grade.name} ${template.fallback}")
            }
        }
    }

    @Test
    fun `시드를 돌면 구간 풀의 모든 마지막 한 줄 틀이 고르게 정해진다`() {
        HUNDREDTHS.values.forEach { hundredths ->
            val pool = punch.poolOf(GiftVerdict(hundredths)).distinctMarkers().markers()
            val assigned = (0 until SEED_SWEEP).map { seed -> punch.candidatesFor(GiftVerdict(hundredths), seed).take(punch.suggestions).markers().single() }
            assertEquals(pool.toSet(), assigned.toSet(), "$hundredths")
            assertTrue(assigned.groupingBy { marker -> marker }.eachCount().values.max() - assigned.groupingBy { marker -> marker }.eachCount().values.min() <= 1)
        }
    }

    @Test
    fun `이야~ 표지는 문장 끝에서만, 줴줴이야~ 안에서는 세지 않는다`() {
        assertEquals(setOf("줴줴이야"), rules.punchMarksIn("굴뚝 앞 유턴. 아 줴줴이야~"))
        assertEquals(setOf("야~"), rules.punchMarksIn("올해 너는 '유턴'이야~"))
        assertEquals(emptySet(), rules.punchMarksIn("너야~ 하고 넘긴 약속이다."))
        assertEquals(null, rules.punchClashIn("굴뚝 앞 유턴. 아 줴줴이야~", "올해 너는 '유턴'이야~"))
    }

    @Test
    fun `마침표 셋 말줄임과 물결표 없는 티원 같은 표기 흔들림도 같은 유행어로 센다`() {
        assertEquals(setOf("엄…"), rules.punchMarksIn("엄... 이걸로 통과다."))
        assertEquals(setOf("티~원"), rules.punchMarksIn("약속 잡자마자 티원."))
        assertEquals(setOf("티~원"), rules.punchMarksIn("약속 잡자마자 티～원."))
        assertEquals(1, rules.slangCountIn("엄... 또 물음표다."))
        assertEquals("엄…", rules.punchClashIn("엄… 판정이다.", "엄... 마지막이다."))
    }

    @Test
    fun `isPunch는 따옴표로 든 본인 말 안의 유행어를 펀치로 세지 않는다`() {
        assertFalse(rules.isPunch("올해 장부에는 '딸깍'뿐."))
        assertFalse(rules.isPunch("올해 장부에는 '요이~'뿐."))
        assertTrue(rules.isPunch("올해 장부 결산, 딸깍."))
        assertEquals(setOf("딸깍"), rules.punchMarksIn("'요이~' 대신 딸깍."))
    }

    @Test
    fun `대체 문장 예비까지 다 겹치면 구간 고정 문장을 쓴다`() {
        val empty = RoastBrief(
            GiftVerdict(TestRoasts.GIFT_SCORE), RoastFacts.EMPTY, RoastQuotes.EMPTY, RoastTopics.EMPTY, RoastLines.EMPTY, BlockedWords.EMPTY,
        )
        val content = RoastSheet.EMPTY.toContent(empty, TestRoasts.catalog.fallback, rules)

        assertEquals(fallbackBand(GiftGrade.SCRAPED_THROUGH).finalLine, content.conclusion?.finalLine?.text)
    }

    private fun fallbackBand(grade: GiftGrade): RoastBandCopy = TestRoasts.catalog.fallback.bandOf(grade)

    companion object {
        /** 서로 다른 첫 추천 틀을 받는 참가자·해 시드 셋. */
        private val SEEDS = listOf(1, 2, 3)

        /** 정해 준 틀이 풀을 고르게 도는지 확인할 시드 수. */
        private const val SEED_SWEEP = 42
        private const val PLAIN_VERDICT = "장부 결산이 끝났다."

        /** 등급마다 그 구간에 드는 총점(100분의 1점). */
        private val HUNDREDTHS = mapOf(
            GiftGrade.PREMIUM to 9500, GiftGrade.GIFT_DEPARTED to 8000, GiftGrade.SCRAPED_THROUGH to 7000,
            GiftGrade.ONE_STEP_SHORT to 6000, GiftGrade.OFF_LIST to 5200, GiftGrade.COAL to 4200,
        )
    }
}
