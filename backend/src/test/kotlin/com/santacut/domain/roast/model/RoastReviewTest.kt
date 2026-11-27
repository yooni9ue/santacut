package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoastAnswers.chipWordOf
import com.santacut.TestRoastAnswers.lineFor
import com.santacut.TestRoastAnswers.evidenceOf
import com.santacut.TestRoastAnswers.habitMessages
import com.santacut.TestRoastAnswers.review
import com.santacut.TestRoastAnswers.slangFor
import com.santacut.TestRoastAnswers.substantialLineIds
import com.santacut.TestRoastAnswers.validJson
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftGrade
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 작가 응답(프롬프트 5판, 리캡 덱) 묶음 단위 검증. 떨어진 필드마다 사유를 남긴다. */
class RoastReviewTest {
    private val gift = TestRoasts.brief(habitMessages())
    private val coal = TestRoasts.brief(habitMessages(), TestRoasts.COAL_SCORE)

    /** 펀치 틀 추천이 없는 입력. 구간 고정 마지막 한 줄(옛 결과·읽을 수 없는 결과용)은 추천 틀과 상관없이 검사한다. */
    private val plainGift = TestRoasts.briefWithoutPunches(habitMessages())
    private val plainCoal = TestRoasts.briefWithoutPunches(habitMessages(), TestRoasts.COAL_SCORE)

    /** 깎인 말에 쓸 나쁜 쪽 칩이 없어 고마워 칩이 남는 입력(같은 말만 되풀이한 본인 말풍선). */
    private val thankless = TestRoasts.brief(TestDossiers.selves(List(THANKLESS_MESSAGES) { "다음 주말에 같이 놀러 가자" }))

    @Test
    fun `규칙을 지킨 응답은 선물·석탄 모두 모든 필드가 통과한다`() {
        assertEquals(emptyList(), review(gift, validJson(gift)).checks.failedFields())
        assertEquals(emptyList(), review(coal, validJson(coal)).checks.failedFields())
    }

    @Test
    fun `슬롯 수는 항목 점수로 정하고 kind별 개수가 다르면 evidence가 떨어진다`() {
        assertEquals(RoastSlots(2, 2, 0), gift.slots)
        assertEquals(RoastSlots(0, 2, 1), coal.slots)

        val checks = review(gift, validJson(gift, evidence = evidenceOf(gift).drop(1))).checks

        assertTrue(EvidenceDrafts.FIELD in checks.failedFields())
        assertTrue(checks.reasonsFor(listOf(EvidenceDrafts.FIELD)).any { reason -> reason.contains("reason 1개(2개여야 한다)") })
    }

    @Test
    fun `상대 줄 번호나 보내지 않은 줄 번호는 line_id로 쓸 수 없다`() {
        val messages = listOf(
            TestDossiers.self(TestDossiers.START, "내일 몇 시에 볼까"),
            TestDossiers.self(TestDossiers.START.plusMinutes(1), "장소는 내가 고를게"),
            TestDossiers.partner(TestDossiers.START.plusMinutes(2), "상대가 보낸 비밀 이야기"),
        )
        val brief = TestRoasts.brief(messages)

        assertFalse(brief.inputText(TestRoasts.mapper).contains("비밀 이야기"))
        assertEquals("내일 몇 시에 볼까", brief.quoteOf(1))
        assertNull(brief.quoteOf(PARTNER_POSITION))
        val draft = EvidenceDraft("reason", PARTNER_POSITION, "", "약속발사상", "판은 늘 여기서 열렸다.")
        assertTrue(draft.problems(brief, TestRoasts.catalog.rules).any { reason -> reason.contains("보낸 본인 줄이 아닌 line_id($PARTNER_POSITION)") })
    }

    @Test
    fun `후보에 없는 chip_key와 앞 카드와 겹친 line_id·chip_key는 그 카드만 떨어진다`() {
        val cards = evidenceOf(gift)
        val unknownChip = cards.mapIndexed { index, card -> if (index == 0) card + ("chip_key" to "made_up") else card }
        val reused = cards.mapIndexed { index, card -> if (index == 1) card + ("line_id" to cards[0]["line_id"]) + ("chip_key" to cards[0]["chip_key"]) else card }

        val unknown = review(gift, validJson(gift, evidence = unknownChip)).checks
        val duplicated = review(gift, validJson(gift, evidence = reused)).checks

        assertEquals(listOf("evidence[0]"), unknown.failedFields())
        assertTrue(unknown.reasonsFor(listOf("evidence")).any { reason -> reason.contains("없는 chip_key(made_up)") })
        assertEquals(listOf("evidence[1]"), duplicated.failedFields())
        val reasons = duplicated.reasonsFor(listOf("evidence"))
        assertTrue(reasons.any { reason -> reason.contains("앞 근거와 같은 line_id") })
        assertTrue(reasons.any { reason -> reason.contains("앞 근거와 같은 chip_key") })
    }

    @Test
    fun `고마워 칩은 점수 깎인 말에만 쓰고 이유·살아남은 말·결론 대표 칩에는 쓰지 않는다`() {
        assertTrue(thankless.hasFact(ThanksFactSpec.KEY))
        val phrase = thankless.facts.keys().first { key -> key.startsWith(PhraseFactSpec.KEY_PREFIX) }
        val lineId = substantialLineIds(thankless).first { id -> thankless.quoteFits(phrase, id) }
        val reasonCard = thanklessCard("reason", lineId, phrase, 0)
        val thanksCut = thanklessCard("cut", null, ThanksFactSpec.KEY, 1)

        val reason = review(thankless, validJson(thankless, evidence = listOf(thanklessCard("reason", null, ThanksFactSpec.KEY, 0), thanksCut))).checks
        assertTrue("evidence[0]" in reason.failedFields(), reason.failedFields().toString())
        assertTrue(reason.reasonsFor(listOf("evidence")).any { text -> text.contains("이 칩(${ThanksFactSpec.KEY})은 reason에 쓰지 않는다") })
        val asCut = review(thankless, validJson(thankless, evidence = listOf(reasonCard, thanksCut), heroLineId = lineId, heroChipKey = phrase)).checks
        assertEquals(emptyList(), asCut.failedFields(), asCut.reasonsFor(listOf("evidence", ConclusionDraft.FIELD)).toString())
        val keep = evidenceOf(coal).map { card -> if (card["kind"] == "keep") card + ("chip_key" to ThanksFactSpec.KEY) else card }
        assertTrue(review(coal, validJson(coal, evidence = keep)).checks.failedFields().any { field -> field.startsWith("evidence[") })
        val hero = review(thankless, validJson(thankless, evidence = listOf(reasonCard, thanksCut), heroChipKey = ThanksFactSpec.KEY)).checks
        assertTrue(ConclusionDraft.FIELD in hero.failedFields(), hero.failedFields().toString())
    }

    @Test
    fun `회귀 - 다른 깎인 말 칩만으로 자리가 차면 고마워 0번은 칩 후보에서 빠지고 판정 한 줄에도 쓰지 못한다`() {
        assertFalse(coal.hasFact(ThanksFactSpec.KEY), coal.facts.keys().toString())
        assertTrue(coal.thanksWordIn("감사 품절. L을 가져가.") != null)
        assertTrue(TextProblems.of("감사 품절. L을 가져가.", RoastTextKind.VERDICT_LINE, coal, TestRoasts.catalog.rules).toList()
            .contains("판정 한 줄은 고마워 칩으로 이끌지 않는다(감사)"))
    }

    private fun thanklessCard(kind: String, lineId: Int?, chipKey: String, index: Int): Map<String, Any?> = mapOf(
        "kind" to kind, "line_id" to lineId, "chip_key" to chipKey, "label" to listOf("주말모집상", "감사품귀상")[index],
        "line" to lineFor(thankless, chipKey, index = index),
    )

    @Test
    fun `작가 입력의 칩 후보는 방향대로 쓸 수 있는 kind를 싣고 고마워 칩은 cut 전용이다`() {
        val chips = TestRoasts.mapper.readTree(thankless.inputText(TestRoasts.mapper)).path("chips").values()
        val thanks = chips.single { chip -> chip.path("chip_key").asString() == ThanksFactSpec.KEY }

        assertEquals(listOf("cut"), thanks.path("kinds").values().map { kind -> kind.asString() })
        chips.forEach { chip ->
            val expected = thankless.directionOf(chip.path("chip_key").asString())?.kindKeys()
            val sent = chip.path("kinds").takeUnless { node -> node.isMissingNode }?.values()?.map { kind -> kind.asString() }
            assertEquals(expected, sent, chip.toString())
        }
    }

    @Test
    fun `결론의 대표 말과 칩은 근거 카드와 같아도 된다`() {
        val first = evidenceOf(gift)[0]
        val json = validJson(gift, heroLineId = first["line_id"] as Int, heroChipKey = first["chip_key"] as String)

        assertEquals(emptyList(), review(gift, json).checks.failedFields())
    }

    @Test
    fun `새 필드의 숫자와 한글 셈말은 숫자 검사에서 떨어진다`() {
        val cards = evidenceOf(gift).mapIndexed { index, card -> if (index == 0) card + ("line" to "물음표를 세 번 보냈다.") else card }
        val json = validJson(gift, verdictLine = "올해 7점 더 받았다.", evidence = cards, title = "두 배 장인", finalLine = "올해 두 배로 바빴다.")

        val checks = review(gift, json).checks

        assertEquals(listOf("verdict_line", "conclusion", "evidence[0]"), checks.failedFields())
        val reasons = checks.reasonsFor(listOf("verdict_line", "conclusion", "evidence"))
        assertTrue(reasons.any { reason -> reason.contains("숫자 표현(7)") })
        assertTrue(reasons.any { reason -> reason.contains("숫자 표현(세 번)") })
        assertTrue(reasons.any { reason -> reason.contains("숫자 표현(두 배)") })
    }

    @Test
    fun `칭찬 뒤집기 말, 영포티 말투, 상 이름이 아닌 라벨, 글자 수 초과는 떨어진다`() {
        val cards = evidenceOf(gift).mapIndexed { index, card ->
            when (index) {
                0 -> card + ("line" to "근데 그 물음표가 방을 살렸다.")
                1 -> card + ("label" to "약속 발사대")
                else -> card + ("line" to "이 방 약속의 달인이다.")
            }
        }
        val json = validJson(gift, verdictLine = "물음표로 판을 깔던 손이 올해 제일 바빴다.", evidence = cards)

        val checks = review(gift, json).checks
        val reasons = checks.reasonsFor(listOf("verdict_line", "evidence"))

        assertTrue(reasons.any { reason -> reason.contains("칭찬 뒤집기 말(근데)") })
        assertTrue(reasons.any { reason -> reason.contains("상 이름이어야 한다") })
        assertTrue(reasons.any { reason -> reason.contains("금지어(") })
        assertTrue(reasons.any { reason -> reason.startsWith("verdict_line 글자 수") })
    }

    @Test
    fun `덱 전체에 허용 유행어를 세 개 넘게 쓰면 deck이 떨어진다`() {
        val cards = evidenceOf(gift).mapIndexed { index, card ->
            card + ("line" to lineFor(gift, card["chip_key"] as String, slang = slangFor(index), index = index))
        }
        val json = validJson(gift, evidence = cards)

        val checks = review(gift, json).checks

        assertEquals(listOf(RoastChecks.DECK_FIELD), checks.failedFields())
    }

    @Test
    fun `근거 한 줄과 마지막 한 줄이 말풍선·칩과 낱말을 나누지 않으면 일반론으로 떨어진다`() {
        val cards = evidenceOf(gift).mapIndexed { index, card -> if (index == 0) card + ("line" to "이건 산타도 인정한다.") else card }
        val json = validJson(gift, evidence = cards, finalLine = "이건 산타도 인정한다.")

        val checks = review(gift, json).checks

        assertEquals(listOf(ConclusionDraft.FIELD, "evidence[0]"), checks.failedFields())
        assertTrue(checks.reasonsFor(listOf("evidence")).any { reason -> reason.contains("line: 일반론") })
        assertTrue(checks.reasonsFor(listOf(ConclusionDraft.FIELD)).any { reason -> reason.contains("final_line: 일반론") })
    }

    @Test
    fun `JSON이 아니면 json 실패 하나만 남고 읽은 값이 없다`() {
        val review = review(gift, "not json")

        assertNull(review.output)
        assertEquals(listOf("json"), review.checks.failedFields())
    }

    @Test
    fun `구간별 대체 문장과 근거 대체 문장은 모두 검증 규칙을 통과한다`() {
        val rules = TestRoasts.catalog.rules
        val fallback = TestRoasts.catalog.fallback
        GiftGrade.entries.forEach { grade ->
            val brief = if (grade.ordinal <= GiftGrade.SCRAPED_THROUGH.ordinal) plainGift else plainCoal
            val band = fallback.bandOf(grade)
            listOf(band.verdictLine, band.plainVerdictLine).forEach { line -> assertNoProblems(line, RoastTextKind.VERDICT_LINE, brief) }
            listOf(band.finalLine, band.plainFinalLine).forEach { line -> assertNoProblems(line, RoastTextKind.FINAL_LINE, brief) }
            assertNoProblems(band.title, RoastTextKind.TITLE, brief)
            assertEquals(0, rules.slangCountIn(band.plainVerdictLine) + rules.slangCountIn(band.plainFinalLine), grade.name)
        }
        fallback.evidence.values.forEach { copy ->
            copy.labels.toList().forEach { label -> assertNoProblems(label, RoastTextKind.LABEL, gift) }
            copy.lines.toList().forEach { line -> assertNoProblems(line, RoastTextKind.EVIDENCE_LINE, gift) }
        }
    }

    private fun assertNoProblems(text: String, kind: RoastTextKind, brief: RoastBrief) =
        assertEquals(emptyList(), TextProblems.of(text, kind, brief, TestRoasts.catalog.rules).toList(), text)

    companion object {
        /** 본인 줄 둘 다음에 온 상대 말풍선의 대화 속 자리. 본인 줄 번호에는 없는 수다. */
        private const val PARTNER_POSITION = 3
        private const val THANKLESS_MESSAGES = 120
    }
}
