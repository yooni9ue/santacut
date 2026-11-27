package com.santacut

import com.santacut.domain.roast.model.EvidenceKind
import com.santacut.domain.roast.model.QuoteRule
import com.santacut.domain.roast.model.QuoteText
import com.santacut.domain.roast.model.RoastBrief
import com.santacut.domain.roast.model.RoastReview
import com.santacut.domain.roast.model.ThanksFactSpec

/**
 * 작가 응답(프롬프트 5판) 테스트가 함께 쓰는 합성 응답 도구. 문장은 모두 검증 규칙을 지키는 중립 예시다.
 */
object TestRoastAnswers {
    private val LABELS = listOf("약속발사상", "질문폭격상", "출발신호상", "웃음폭주상", "답장독촉상")
    private val HANGUL_WORD = Regex("[가-힣]{2,}")
    private const val WORD = "{word}"
    private val SLANGS = listOf("요이~", "티~원", "엄…", "예?")

    /** 카드마다 낱말이 겹치지 않는 근거 한 줄 틀. 모두 완결된 문장 끝으로 맺는다. */
    private val LINE_TEMPLATES = listOf(
        "{word} 발사로 방이 굴러갔다.", "{word} 들고 판을 깔았다.", "{word} 장전 소리가 요란하다.", "{word} 기록은 산타도 본다.", "{word} 습관이 또 터졌다.",
    )

    /** 펀치 틀 표지를 문장에 쓸 꼴. 줄기가 활용형 앞부분인 유행어만 적는다. */
    private val PUNCH_WORDS = mapOf("체포하겠" to "체포하겠어.", "넌 나가" to "넌 나가라.", "넌 남아" to "넌 남아라.")

    /** 작가에게 보낸 본인 줄 번호(line_id). */
    fun sentLineIds(brief: RoastBrief): List<Int> =
        TestRoasts.mapper.readTree(brief.inputText(TestRoasts.mapper)).path("transcript").values()
            .flatMap { act -> act.path("lines").values().map { line -> line.asString().substringBefore("|").toInt() } }

    /** 칩 [chipKey] 이름의 첫 두 글자 이상 한글 낱말. 한 줄이 칩과 낱말을 나누게(일반론 검사) 문장에 넣는다. */
    fun chipWordOf(brief: RoastBrief, chipKey: String): String =
        brief.chipOf(chipKey)?.label?.let { label -> HANGUL_WORD.find(label)?.value }.orEmpty()

    /**
     * 칩 [chipKey] 낱말을 받아 쓴 근거 카드 한 줄. [index]번째 카드마다 다른 틀을 써 카드끼리 낱말이 겹치지 않고,
     * 완결된 문장 끝으로 맺는다. [slang]이 있으면 그 유행어 표기를 끝에 붙인다.
     */
    fun lineFor(brief: RoastBrief, chipKey: String, slang: String? = null, index: Int = 0): String =
        LINE_TEMPLATES[index % LINE_TEMPLATES.size].replace(WORD, chipWordOf(brief, chipKey)).let { line ->
            if (slang == null) line else line.removeSuffix(".") + ", $slang"
        }

    /** 카드마다 다른 유행어(같은 유행어를 두 카드에 쓰면 낱말 되풀이로 떨어진다). */
    fun slangFor(index: Int): String = SLANGS[index % SLANGS.size]

    /** 근거 말풍선으로 쓸 만한(한글 네 음절 이상) 본인 줄 번호. */
    fun substantialLineIds(brief: RoastBrief): List<Int> =
        sentLineIds(brief).filter { lineId -> brief.quoteOf(lineId)?.let(QuoteText::isSubstantial) == true }

    /**
     * [brief] 슬롯 수만큼 서로 다른 본인 줄과 칩으로 채운 근거 카드. 한 줄은 칩 낱말을 받아 쓴다.
     * 본인 줄은 근거로 쓸 만한 줄 가운데 아직 쓰지 않았고 그 칩의 말풍선 조건([RoastBrief.quoteFits])에 맞는 줄이다.
     * 말풍선으로 보여 줄 수 없는 칩([QuoteRule.NONE])은 줄 없이 쓴다. 맞는 줄이 없는 칩은 건너뛴다.
     */
    fun evidenceOf(brief: RoastBrief): List<Map<String, Any?>> {
        val lineIds = substantialLineIds(brief)
        val kinds = List(brief.slots.reason) { EvidenceKind.REASON } + List(brief.slots.cut) { EvidenceKind.CUT } +
            List(brief.slots.keep) { EvidenceKind.KEEP }
        val used = mutableSetOf<Int>()
        val chips = brief.facts.keys().filterNot { key -> key == ThanksFactSpec.KEY }.toMutableList()
        return kinds.mapIndexed { index, kind ->
            val (chipKey, lineId) = chips.firstNotNullOf { key ->
                if (brief.quoteRuleOf(key) == QuoteRule.NONE) key to null
                else lineIds.firstOrNull { id -> id !in used && brief.quoteFits(key, id) }?.let { id -> key to id }
            }
            chips -= chipKey
            lineId?.let(used::add)
            mapOf("kind" to kind.key, "line_id" to lineId, "chip_key" to chipKey, "label" to LABELS[index], "line" to lineFor(brief, chipKey, index = index))
        }
    }

    /** 대표 말과 대표 칩. 대표 칩의 말풍선 조건에 맞는 본인 줄이 있는 첫 칩과 그 줄(뒤의 줄부터 찾는다)이다. */
    fun heroOf(brief: RoastBrief): Pair<Int, String> {
        val lineIds = substantialLineIds(brief).reversed()
        return brief.facts.keys().filterNot { key -> key == ThanksFactSpec.KEY }.firstNotNullOf { key ->
            lineIds.firstOrNull { id -> brief.quoteFits(key, id) }?.let { id -> id to key }
        }
    }

    /**
     * 추천 펀치 틀 가운데 판정 한 줄 [verdictLine]과 겹치지 않는 첫 틀로 쓴 마지막 한 줄. 대표 칩 [heroChipKey] 낱말을 받아 쓴다.
     * 추천이 없으면 유행어 하나로 맺는다.
     */
    fun finalLineFor(brief: RoastBrief, heroChipKey: String, verdictLine: String): String {
        val word = chipWordOf(brief, heroChipKey)
        val marker = brief.suggestedPunches.markers().firstOrNull { mark -> TestRoasts.catalog.rules.punchClashIn(verdictLine, mark) == null }
            ?: return "$word 장부 결산, 요이~"
        return if (marker == "야~") "올해 너는 $word 장부야~" else "$word 장부 결산. ${PUNCH_WORDS[marker] ?: marker}"
    }

    /** 규칙을 지킨 응답 JSON. 필드마다 바꿔 넣을 수 있다. */
    fun validJson(
        brief: RoastBrief,
        verdictLine: String = "올해 장부 결산, 딸깍.",
        evidence: List<Map<String, Any?>> = evidenceOf(brief),
        title: String = "약속 발사대",
        heroLineId: Int? = heroOf(brief).first,
        heroChipKey: String = heroOf(brief).second,
        finalLine: String = finalLineFor(brief, heroChipKey, verdictLine),
    ): String = TestRoasts.mapper.writeValueAsString(
        mapOf(
            "verdict_line" to verdictLine,
            "evidence" to evidence,
            "conclusion" to mapOf("title" to title, "hero_line_id" to heroLineId, "hero_chip_key" to heroChipKey, "final_line" to finalLine),
        ),
    )

    fun review(brief: RoastBrief, json: String): RoastReview = RoastReview.of(json, brief, TestRoasts.catalog.rules, TestRoasts.mapper)

    /** 습관이 드러나는 본인 말풍선 150개(다섯 가지 말을 돌려 씀). 입버릇이 든 말은 근거로 쓸 만한 길이(한글 네 음절 이상)다. */
    fun habitMessages() = TestDossiers.selves(
        List(150) { index -> listOf("내일 저녁 ㄱ?", "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ 이거 웃기다", "언제 볼까?", "ㄱㄱ 바로 출발", "넹")[index % 5] },
        minutesApart = 3,
    )
}
