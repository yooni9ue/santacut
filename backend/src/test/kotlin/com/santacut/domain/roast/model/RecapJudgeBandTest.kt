package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.analysis.model.GiftVerdict
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 리캡 판정 한 줄·결론의 구간 맞춤과 틀 쏠림(심사 지적 A, B, F). 구간 이름과 구간 밈은 그 구간만 쓰고, 판정 한 줄·마지막 한 줄 틀은 사람마다 하나씩 정해 준다.
 * 회귀 예문은 심사 덱에서 실제로 나왔던 문장을 합성 입력에 옮긴 것이다(대화 원문은 쓰지 않는다).
 */
class RecapJudgeBandTest {
    private val catalog = TestRoasts.catalog
    private val rules = catalog.rules
    private val punch = catalog.punch

    @Test
    fun `모든 구간의 판정·마지막 한 줄 틀과 대체 문장은 다른 구간의 표현을 쓰지 않는다`() {
        assertEquals(emptyList(), punch.violations(rules))
        GiftGrade.entries.forEach { grade ->
            val foreign = punch.foreignMarksFor(grade)
            val brief = briefOf(scoreOf(grade))
            (punch.poolOf(brief.verdict).fallbacks() + punch.verdicts.of(grade.name).fallbacks()).forEach { text ->
                assertNull(foreign.foundIn(text), "${grade.name}: $text")
            }
            assertNull(foreign.foundIn(catalog.fallback.bandOf(grade).finalLine(plain = true)), grade.name)
        }
    }

    @Test
    fun `판정 틀은 빈칸에 6자 이상 여유를 남기고 판정·마지막 한 줄 대체 문장은 모두 글자 수 범위 안이다`() {
        GiftGrade.entries.forEach { grade ->
            punch.verdicts.of(grade.name).toList().forEach { template ->
                val bare = BLANK.replace(template.template, "")
                val room = RoastTextKind.VERDICT_LINE.maxLength - bare.codePointCount(0, bare.length)
                assertTrue(room >= MIN_VERDICT_ROOM, "${grade.name} ${template.template} 여유 $room")
                assertNull(RoastTextKind.VERDICT_LINE.lengthProblemOf(template.fallback), template.fallback)
            }
            punch.poolOf(GiftVerdict(scoreOf(grade))).fallbacks().forEach { line ->
                assertNull(RoastTextKind.FINAL_LINE.lengthProblemOf(line), line)
            }
        }
    }

    @Test
    fun `마지막 한 줄 틀은 모두 빈칸에 6자 이상 여유를 남긴다`() {
        (punch.bands.pools() + punch.skins.pools()).flatMap(PunchTemplates::toList).forEach { template ->
            val bare = BLANK.replace(template.template, "")
            val room = RoastTextKind.FINAL_LINE.maxLength - bare.codePointCount(0, bare.length)
            assertTrue(room >= MIN_VERDICT_ROOM, "${template.template} 여유 $room")
        }
    }

    @Test
    fun `작가 입력에 이 사람 구간이 쓸 수 없는 표현을 banned_marks로 싣고 명단에 없다는 말은 막지 않는다`() {
        val banned = briefOf(DEPARTED).input().bannedMarks

        assertTrue("프리패스" in banned, banned.toString())
        assertTrue("체포하겠" in banned, banned.toString())
        assertTrue("넌 남아" in banned, banned.toString())
        assertFalse("딸깍" in banned, banned.toString())
        GiftGrade.entries.forEach { grade -> assertNull(punch.foreignMarksFor(grade).foundIn("이름이 명단에 없다고요."), grade.name) }
    }

    @Test
    fun `회귀 - 65점대 덱의 출발 줴줴이야는 다른 구간 밈이고 판정 자리를 입버릇 하나로 채워 떨어진다`() {
        val brief = briefOf(SCRAPED, phrase("chulbal", "출발"))

        val reasons = TextProblems.of("출발 줴줴이야~", RoastTextKind.VERDICT_LINE, brief, rules).toList()

        assertTrue(reasons.contains("다른 구간의 표현(줴줴이야)"), reasons.toString())
        assertTrue(reasons.any { reason -> reason.startsWith("판정 자리가 비었거나 입버릇만 넣었다") }, reasons.toString())
    }

    @Test
    fun `회귀 - 50점대 덱은 석탄 구간의 체포하겠어를 판정 한 줄에도 마지막 한 줄에도 쓰지 못한다`() {
        val brief = briefOf(OFF_LIST)

        listOf(RoastTextKind.VERDICT_LINE, RoastTextKind.FINAL_LINE).forEach { kind ->
            val reasons = TextProblems.of("감사 품절 혐의로 체포하겠어.", kind, brief, rules).toList()
            assertTrue(reasons.contains("다른 구간의 표현(체포하겠)"), "$kind $reasons")
        }
        assertNull(briefOf(COAL).foreignMarkIn("감사 품절 혐의로 체포하겠어."))
    }

    @Test
    fun `회귀 - 75점대 덱은 90점 구간 이름인 프리패스를 마지막 한 줄과 칭호에 쓰지 못한다`() {
        val brief = briefOf(DEPARTED)

        assertTrue(TextProblems.of("'굿'으로 썰매 프리패스.", RoastTextKind.FINAL_LINE, brief, rules).toList().contains("다른 구간의 표현(프리패스)"))
        assertTrue(TextProblems.of("굴뚝프리패스", RoastTextKind.TITLE, brief, rules).toList().contains("다른 구간의 표현(프리패스)"))
        assertNull(briefOf(PREMIUM).foreignMarkIn("'굿'으로 굴뚝 프리패스."))
    }

    @Test
    fun `작가 입력에는 이 사람 구간의 판정 틀 하나만 싣는다`() {
        GiftGrade.entries.forEach { grade ->
            val brief = briefOf(scoreOf(grade))
            val sent = brief.input().verdictTemplate

            assertTrue(sent in punch.verdicts.of(grade.name).hints(), "${grade.name}: $sent")
        }
    }

    @Test
    fun `판정 한 줄은 정해 준 판정 틀의 펀치 말을 써야 한다`() {
        val brief = briefOf(COAL, seed = 0)
        val assigned = assertNotNull(brief.verdictPunch)
        val other = punch.verdicts.of(GiftGrade.COAL.name).toList().first { template -> template.marker != assigned.marker }

        val reasons = TextProblems.of(other.fallback, RoastTextKind.VERDICT_LINE, brief, rules).toList()

        assertTrue(reasons.any { reason -> reason.startsWith("정해 준 판정 틀(verdict_template)을 쓰지 않았다") }, reasons.toString())
        assertFalse(TextProblems.of("뭐임 남발 결산, ${wordOf(assigned)}", RoastTextKind.VERDICT_LINE, brief, rules).toList().any { reason ->
            reason.startsWith("정해 준 판정 틀")
        })
    }

    @Test
    fun `이웃한 시드는 서로 다른 마지막 한 줄 틀을 받고 판정 틀은 그 틀과 다른 유행어 가족이다`() {
        GiftGrade.entries.forEach { grade ->
            val verdict = GiftVerdict(scoreOf(grade))
            (0 until NEIGHBOURS).forEach { seed ->
                val finals = listOf(seed, seed + 1).map { neighbour -> punch.candidatesFor(verdict, neighbour).take(1).hints().single() }
                assertNotEquals(finals[0], finals[1], "${grade.name} 시드 $seed")
                val assigned = assertNotNull(punch.verdictFor(verdict, seed, rules))
                val finalPunch = punch.candidatesFor(verdict, seed).toList().first()
                assertNull(rules.punchClashIn(assigned.fallback, finalPunch.fallback), "${grade.name} 시드 $seed")
                assertNotEquals(assigned.marker, finalPunch.marker)
            }
        }
        val verdicts = (0 until NEIGHBOURS).map { seed -> punch.verdictFor(GiftVerdict(COAL), seed, rules)?.marker }
        assertTrue(verdicts.toSet().size > 1, verdicts.toString())
    }

    @Test
    fun `회귀 - 다섯 덱 가운데 셋이 올해 너는 OO야를 쓰던 쏠림은 시드 다섯이 서로 다른 마지막 한 줄 틀을 받아 사라진다`() {
        listOf(SCRAPED, COAL, OFF_LIST).forEach { hundredths ->
            val firsts = (0 until NEIGHBOURS).map { seed -> punch.candidatesFor(GiftVerdict(hundredths), seed).take(1).markers().single() }
            assertTrue(firsts.count { marker -> marker == "야~" } <= 1, firsts.toString())
        }
    }

    @Test
    fun `회귀 - 프롬프트 예시를 그대로 옮긴 칭호와 마지막 한 줄은 떨어진다`() {
        val brief = briefOf(SCRAPED)

        assertTrue(TextProblems.copiedExample("약속발사대", brief).toList().any { reason -> reason.startsWith("예시 문장을 그대로 옮겼다") })
        assertTrue(TextProblems.copiedExample("올해 너는 'ㄱ?'이야~", brief).toList().any { reason -> reason.startsWith("예시 문장") })
    }

    @Test
    fun `대체 문장을 그대로 옮긴 근거 한 줄도 예시 베끼기로 떨어진다`() {
        val brief = briefOf(SCRAPED)
        val fallbackLine = catalog.fallback.evidence.getValue(EvidenceKind.REASON.key).lines.toList().first()

        assertTrue(TextProblems.copiedExample(fallbackLine, brief).toList().any { reason -> reason.startsWith("예시 문장") })
        assertTrue(brief.isExample(fallbackLine.replace(" ", "")))
    }

    @Test
    fun `컷과 1점 안이면 위·아래 어느 쪽이든 아슬아슬하다는 말을 작가 입력에 싣는다`() {
        assertEquals("컷 바로 위 0.13점 차", briefOf(SCRAPED).nearMiss())
        assertEquals("컷 바로 아래 0.40점 차", briefOf(JUST_BELOW).nearMiss())
        assertNull(briefOf(DEPARTED).nearMiss())
        assertEquals("컷 바로 위 0.13점 차", briefOf(SCRAPED).input().nearMiss)
    }

    @Test
    fun `회귀 - 고마워 0번이 있으면 판정 한 줄은 고맙다는 말로 덱을 이끌지 못한다`() {
        val withThanks = briefOf(COAL, thanks())

        assertTrue(TextProblems.of("감사 품절. L을 가져가.", RoastTextKind.VERDICT_LINE, withThanks, rules).toList().contains("판정 한 줄은 고마워 칩으로 이끌지 않는다(감사)"))
        assertNull(briefOf(COAL).thanksWordIn("감사 품절. L을 가져가."))
    }

    @Test
    fun `회귀 - 라벨과 칭호가 발사 같은 줄기를 덱 안에서 되풀이하면 뒤의 것이 떨어진다`() {
        assertEquals(listOf("덱 안에서 같은 줄기를 되풀이했다(발사)"), TextProblems.repeatedStem("약속발사상", listOf("감사발사상"), rules).toList())
        assertEquals(listOf("덱 안에서 같은 줄기를 되풀이했다(품절)"), TextProblems.repeatedStem("감사품절왕", listOf("물음표품절상"), rules).toList())
        assertEquals(emptyList(), TextProblems.repeatedStem("물음표총대상", listOf("감사발사상"), rules).toList())
        val conclusion = ConclusionDraft("뭐임발사대", null, null, "뭐임 살포 혐의로 체포하겠어.")
        val evidence = EvidenceDrafts(listOf(EvidenceDraft("cut", null, "x", "뭐임발사상", "뭐임으로 방을 얼렸다.")))
        assertTrue(conclusion.check(briefOf(COAL), rules, evidence = evidence).reasons.contains("title: 덱 안에서 같은 줄기를 되풀이했다(발사)"))
    }

    private fun wordOf(template: PunchTemplate): String = PUNCH_WORDS[template.marker] ?: template.marker

    private fun briefOf(hundredths: Int, vararg facts: RoastFact, seed: Int = 0): RoastBrief {
        val verdict = GiftVerdict(hundredths)
        return RoastBrief(
            verdict, RoastFacts(facts.toList()), RoastQuotes.EMPTY, RoastTopics.EMPTY, RoastLines.EMPTY, BlockedWords.EMPTY,
            slang = rules.slang.allowed, verdictPunch = punch.verdictFor(verdict, seed, rules), foreignMarks = punch.foreignMarksFor(verdict.grade()),
            examples = catalog.examples, thanksWords = catalog.thanks.words,
        )
    }

    private fun phrase(key: String, display: String): RoastFact = RoastFact(
        PhraseFactSpec.KEY_PREFIX + key, "'$display'라고 보낸 횟수", "남들보다 많음", "habit", RoastChip("'$display'", "43번"), 0.9,
        direction = catalog.phrase.directionOf(key),
    )

    private fun thanks(): RoastFact =
        RoastFact(ThanksFactSpec.KEY, catalog.thanks.meaning, catalog.thanks.level, catalog.thanks.group, RoastChip("'고마워'", "0번"), direction = ChipDirection.BAD)

    private fun scoreOf(grade: GiftGrade): Int = (0..MAX_HUNDREDTHS step SCORE_STEP).first { hundredths -> GiftGrade.of(hundredths) == grade }

    companion object {
        private const val PREMIUM = 9300
        private const val DEPARTED = 7699
        private const val SCRAPED = 6513
        private const val JUST_BELOW = 6460
        private const val OFF_LIST = 5259
        private const val COAL = 4915
        private const val NEIGHBOURS = 5
        private const val MAX_HUNDREDTHS = 10_000
        private const val SCORE_STEP = 100
        private const val MIN_VERDICT_ROOM = 6
        private val BLANK = Regex("'?[(][^)]*[)]'?")
        private val PUNCH_WORDS = mapOf(
            "체포하겠" to "체포하겠어.", "넌 나가" to "넌 나가라.", "넌 남아" to "넌 남아라.", "영원할 건가" to "영원할 건가 보다.", "L을 가져가" to "L을 가져가.",
        )
    }
}
