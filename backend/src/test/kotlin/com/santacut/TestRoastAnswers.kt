package com.santacut

import com.santacut.domain.roast.model.EvidenceKind
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

    /** 작가에게 보낸 본인 줄 번호(line_id). */
    fun sentLineIds(brief: RoastBrief): List<Int> =
        TestRoasts.mapper.readTree(brief.inputText(TestRoasts.mapper)).path("transcript").values()
            .flatMap { act -> act.path("lines").values().map { line -> line.asString().substringBefore("|").toInt() } }

    /** 칩 [chipKey] 이름의 첫 두 글자 이상 한글 낱말. 한 줄이 칩과 낱말을 나누게(일반론 검사) 문장에 넣는다. */
    fun chipWordOf(brief: RoastBrief, chipKey: String): String =
        brief.chipOf(chipKey)?.label?.let { label -> HANGUL_WORD.find(label)?.value }.orEmpty()

    /** 칩 [chipKey] 낱말을 받아 쓴 근거 카드 한 줄. [slang]이면 유행어 하나를 붙인다. */
    fun lineFor(brief: RoastBrief, chipKey: String, slang: Boolean = false): String =
        "장부에 적힌 ${chipWordOf(brief, chipKey)} 버릇" + (if (slang) ", 딸깍." else ".")

    /** 근거 말풍선으로 쓸 만한(한글 네 음절 이상) 본인 줄 번호. */
    fun substantialLineIds(brief: RoastBrief): List<Int> =
        sentLineIds(brief).filter { lineId -> brief.quoteOf(lineId)?.let(QuoteText::isSubstantial) == true }

    /**
     * [brief] 슬롯 수만큼 서로 다른 본인 줄과 칩으로 채운 근거 카드. 점수 깎인 말도 본인 줄을 쓴다. 한 줄은 칩 낱말을 받아 쓴다.
     * 본인 줄은 근거로 쓸 만한 줄 가운데 아직 쓰지 않은 줄이고, 입버릇 칩이면 그 입버릇이 든 줄을 고른다.
     */
    fun evidenceOf(brief: RoastBrief): List<Map<String, Any?>> {
        val lineIds = substantialLineIds(brief)
        val chips = brief.facts.keys().filterNot { key -> key == ThanksFactSpec.KEY }
        val kinds = List(brief.slots.reason) { EvidenceKind.REASON } + List(brief.slots.cut) { EvidenceKind.CUT } +
            List(brief.slots.keep) { EvidenceKind.KEEP }
        val used = mutableSetOf<Int>()
        return kinds.mapIndexed { index, kind ->
            val lineId = lineIds.first { id -> id !in used && brief.quoteHasPhrase(chips[index], brief.quoteOf(id).orEmpty()) }
            used += lineId
            mapOf("kind" to kind.key, "line_id" to lineId, "chip_key" to chips[index], "label" to LABELS[index], "line" to lineFor(brief, chips[index]))
        }
    }

    /** 규칙을 지킨 응답 JSON. 필드마다 바꿔 넣을 수 있다. */
    fun validJson(
        brief: RoastBrief,
        verdictLine: String = "올해 약속 발사대, 딸깍.",
        evidence: List<Map<String, Any?>> = evidenceOf(brief),
        title: String = "약속 발사대",
        heroLineId: Int = sentLineIds(brief).last(),
        heroChipKey: String = brief.facts.keys().first(),
        finalLine: String = "${chipWordOf(brief, heroChipKey)} 장부는 올해도 딸깍.",
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
