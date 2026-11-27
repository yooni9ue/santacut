package com.santacut.domain.roast.model

import com.santacut.TestRecapFacts.COAL
import com.santacut.TestRecapFacts.ROASTER_LINES
import com.santacut.TestRecapFacts.briefOf
import com.santacut.TestRecapFacts.roasterFacts
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.analysis.model.GiftVerdict
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 리캡 5차 심사 지적 4·5·7: 구간 밈을 명세 5절 구간 표대로 그 구간에만 쓰게 하고, 마지막 한 줄·칭호의 틀 쏠림과 명세 예문 베끼기,
 * 구간 이름을 되풀이하거나 점수와 어긋나는 칭호, 인격 단정어, 꾸밈말 라벨과 따옴표 라벨을 막는다. 회귀 예문은 4차 실측 덱의 문장이다.
 */
class RecapBandWordingTest {
    private val catalog = TestRoasts.catalog
    private val rules = catalog.rules
    private val punch = catalog.punch

    @Test
    fun `구간 밈은 명세 5절 구간 표의 구간에서만 쓴다`() {
        OWNED.forEach { (mark, owner) ->
            GiftGrade.entries.forEach { grade ->
                val found = punch.foreignMarksFor(grade).foundIn(SAMPLES.getValue(mark))
                if (grade == owner) assertNull(found, "${grade.name} $mark") else assertEquals(mark, found, "${grade.name} $mark")
            }
        }
    }

    @Test
    fun `회귀 - 49점 이하 덱의 감정가뭄 탓에 줴줴이야는 55~64점 구간 밈이라 판정 한 줄에서 떨어진다`() {
        val brief = briefOf(COAL, roasterFacts(), ROASTER_LINES)

        val reasons = TextProblems.of("감정가뭄 탓에 줴줴이야~", RoastTextKind.VERDICT_LINE, brief, rules).toList()

        assertTrue(reasons.contains("다른 구간의 표현(줴줴이야)"), reasons.toString())
        assertNull(briefOf(ONE_STEP, roasterFacts(), ROASTER_LINES).foreignMarkIn("감정가뭄 탓에 줴줴이야~"))
    }

    @Test
    fun `엄…은 마침표 셋으로 써도 구간 밈으로 보고 엄마 같은 낱말은 보지 않는다`() {
        val coal = punch.foreignMarksFor(GiftGrade.COAL)

        assertEquals("엄…", coal.foundIn("엄... 또 재촉이다."))
        assertNull(coal.foundIn("엄마한테 혼나는 소리다."))
    }

    @Test
    fun `회귀 - 올해 너는 OO이야 틀은 65~74점 구간에만 있고 풀 하나에 한 번까지다`() {
        GiftGrade.entries.filterNot { grade -> grade == GiftGrade.SCRAPED_THROUGH }.forEach { grade ->
            val verdict = GiftVerdict(scoreOf(grade))
            assertFalse(punch.poolOf(verdict).markers().contains(FAMILY), grade.name)
            assertFalse(punch.verdicts.of(grade.name).markers().contains(FAMILY), grade.name)
            assertEquals("올해 너는 ○○이야~", punch.foreignMarksFor(grade).foundIn("올해 너는 '와'이야~"), grade.name)
        }
        (punch.bands.pools() + punch.skins.pools() + punch.verdicts.pools()).forEach { pool ->
            assertTrue(pool.markers().count { marker -> marker == FAMILY } <= 1, pool.markers().toString())
        }
    }

    @Test
    fun `회귀 - 명세 예문 꼴의 산타도 인정 마지막 한 줄은 떨어진다`() {
        val brief = briefOf(DEPARTED, roasterFacts(), ROASTER_LINES, RoastSlots(2, 1, 0))

        val reasons = TextProblems.of("따뜻한 말은 산타도 인정. 넌 남아라.", RoastTextKind.FINAL_LINE, brief, rules).toList()

        assertTrue(reasons.contains("예시 상투 문구(산타도 인정)"), reasons.toString())
        assertTrue(reasons.contains("다른 구간의 표현(넌 남아)"), reasons.toString())
        val texts = (punch.bands.pools() + punch.skins.pools() + punch.verdicts.pools()).flatMap { pool -> pool.hints() + pool.fallbacks() }
        assertTrue(texts.none { text -> rules.stockPhraseIn(text) != null }, texts.toString())
    }

    @Test
    fun `회귀 - 구간 이름을 되풀이하거나 왕·장인으로 끝나는 칭호는 떨어진다`() {
        val offList = briefOf(OFF_LIST, roasterFacts(), ROASTER_LINES)
        val coal = briefOf(COAL, roasterFacts(), ROASTER_LINES)

        assertTrue(titleProblems("명단 실종자", offList).contains("구간 이름을 되풀이한 칭호(명단 실종)"))
        assertTrue(titleProblems("재촉 왕", coal).contains("칭호 끝말 금지(왕)"))
        assertTrue(titleProblems("단답 왕", coal).contains("칭호 끝말 금지(왕)"))
        assertTrue(titleProblems("뭐임 장인", coal).contains("칭호 끝말 금지(장인)"))
        assertEquals(emptyList(), titleProblems("옷 감별사", coal))
    }

    @Test
    fun `회귀 - 컷 위 8점에 턱걸이 칭호는 점수와 어긋나 떨어지고 작가가 near_miss를 받는 1점 안에서만 쓴다`() {
        val far = briefOf(FAR_ABOVE_CUT, roasterFacts(), ROASTER_LINES, RoastSlots(2, 1, 0))
        val justOutside = briefOf(JUST_OUTSIDE_NEAR_MISS, roasterFacts(), ROASTER_LINES, RoastSlots(2, 1, 0))
        val close = briefOf(CLOSE_ABOVE_CUT, roasterFacts(), ROASTER_LINES, RoastSlots(2, 1, 0))

        assertTrue(titleProblems("턱걸이 장인", far).contains("점수와 어긋난 칭호(턱걸이, 컷과 붙지 않았다)"))
        assertTrue(titleProblems("굴뚝 턱걸이", far).contains("점수와 어긋난 칭호(턱걸이, 컷과 붙지 않았다)"))
        assertTrue(titleProblems("굴뚝 턱걸이", justOutside).contains("점수와 어긋난 칭호(턱걸이, 컷과 붙지 않았다)"))
        assertEquals(null, justOutside.nearMiss())
        assertEquals(emptyList(), titleProblems("굴뚝 턱걸이", close))
        assertTrue(close.nearMiss() != null)
    }

    @Test
    fun `회귀 - 감사 인사를 남기지 않는 냉정함 같은 인격 단정어는 떨어진다`() {
        val brief = briefOf(COAL, roasterFacts(), ROASTER_LINES)

        val reasons = TextProblems.of("감사 인사가 없는 냉정함이다.", RoastTextKind.EVIDENCE_LINE, brief, rules).toList()

        assertTrue(reasons.any { reason -> reason.startsWith("인격 단정어(냉정") }, reasons.toString())
    }

    @Test
    fun `회귀 - 따뜻한 상 같은 꾸밈말 라벨과 따옴표 라벨은 떨어지고 행동 이름 상은 통과한다`() {
        val brief = briefOf(COAL, roasterFacts(), ROASTER_LINES)

        assertTrue(labelProblems("따뜻한 상", brief).any { reason -> reason.startsWith("라벨이 꾸밈말로 끝난다") })
        assertTrue(labelProblems("'했어?'생존상", brief).any { reason -> reason.startsWith("라벨에 따옴표를 쓰지 않는다") })
        assertEquals(emptyList(), labelProblems("말수 미달상", brief))
        assertEquals(emptyList(), labelProblems("칭찬투척상", brief))
    }

    @Test
    fun `저장 라벨은 띄어쓰기와 따옴표·문장 부호를 뗀다`() {
        assertEquals("말수미달상", RoastEvidence.labelOf("말수 미달상"))
        assertEquals("했어생존상", RoastEvidence.labelOf("'했어?'생존상"))
        val brief = briefOf(COAL, roasterFacts(), ROASTER_LINES)
        val stored = EvidenceDraft("cut", 1, PhraseFactSpec.KEY_PREFIX + "mwoim", "옷 평가상", "옷 보자마자 뭐임부터 날린다.").toEvidence(brief)
        assertEquals("옷평가상", stored?.label)
    }

    @Test
    fun `대체 칭호는 구간 이름·금지 끝말·점수 어긋남 검사를 모두 통과한다`() {
        GiftGrade.entries.forEach { grade ->
            val title = catalog.fallback.bandOf(grade).title
            val brief = briefOf(scoreOf(grade), roasterFacts(), ROASTER_LINES)
            assertEquals(emptyList(), titleProblems(title, brief), "${grade.name} $title")
        }
    }

    private fun titleProblems(title: String, brief: RoastBrief): List<String> = TextProblems.of(title, RoastTextKind.TITLE, brief, rules).toList()

    private fun labelProblems(label: String, brief: RoastBrief): List<String> = TextProblems.of(label, RoastTextKind.LABEL, brief, rules).toList()

    private fun scoreOf(grade: GiftGrade): Int = (0..MAX_HUNDREDTHS step SCORE_STEP).first { hundredths -> GiftGrade.of(hundredths) == grade }

    companion object {
        private const val DEPARTED = 7701
        private const val ONE_STEP = 6000
        private const val OFF_LIST = 5192
        private const val FAR_ABOVE_CUT = 7300
        private const val CLOSE_ABOVE_CUT = 6560
        private const val JUST_OUTSIDE_NEAR_MISS = 6620
        private const val MAX_HUNDREDTHS = 10_000
        private const val SCORE_STEP = 100
        private const val FAMILY = "야~"

        /** 명세 5절 구간 표의 구간 밈과 그 밈을 가진 구간. */
        private val OWNED = listOf(
            "요이~" to GiftGrade.PREMIUM, "넌 남아" to GiftGrade.PREMIUM, "딸깍" to GiftGrade.GIFT_DEPARTED,
            "아뇨아뇨아뇨" to GiftGrade.SCRAPED_THROUGH, "올해 너는 ○○이야~" to GiftGrade.SCRAPED_THROUGH,
            "줴줴이야" to GiftGrade.ONE_STEP_SHORT, "엄…" to GiftGrade.ONE_STEP_SHORT,
            "예?" to GiftGrade.OFF_LIST, "영원할 건가" to GiftGrade.OFF_LIST,
            "체포하겠" to GiftGrade.COAL, "L을 가져가" to GiftGrade.COAL, "퇴사할게여" to GiftGrade.COAL,
        )

        /** 구간 밈마다 그 밈만 든 예문. */
        private val SAMPLES = mapOf(
            "요이~" to "장부 맨 윗줄. 요이~", "넌 남아" to "약속은 인정. 넌 남아라.", "딸깍" to "약속 잡기, 딸깍.",
            "아뇨아뇨아뇨" to "나쁘냐고? 아뇨아뇨아뇨.", "올해 너는 ○○이야~" to "올해 너는 '와'이야~",
            "줴줴이야" to "답장 줴줴이야~", "엄…" to "엄… 답장이 없다.", "예?" to "이게 답장이라고요? 예?",
            "영원할 건가" to "재촉, 영원할 건가 보다.", "체포하겠" to "재촉 혐의로 체포하겠어.", "L을 가져가" to "답장 대결은 L을 가져가.",
            "퇴사할게여" to "명단 퇴사할게여.",
        )
    }
}
