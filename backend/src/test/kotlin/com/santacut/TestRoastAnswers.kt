package com.santacut

import com.santacut.domain.roast.model.EvidenceKind
import com.santacut.domain.roast.model.RoastBrief
import com.santacut.domain.roast.model.RoastReview
import com.santacut.domain.roast.model.ThanksFactSpec

/**
 * 작가 응답(프롬프트 5판) 테스트가 함께 쓰는 합성 응답 도구. 문장은 모두 검증 규칙을 지키는 중립 예시다.
 */
object TestRoastAnswers {
    private val LABELS = listOf("약속발사상", "질문폭격상", "출발신호상", "웃음폭주상", "답장독촉상")
    private val LINES = listOf("판은 늘 여기서 열렸다.", "물음표로 방을 깨웠다.", "출발 신호는 늘 빨랐다.", "웃음 연타는 감점이다.", "재촉은 산타도 봤다.")

    /** 작가에게 보낸 본인 줄 번호(line_id). */
    fun sentLineIds(brief: RoastBrief): List<Int> =
        TestRoasts.mapper.readTree(brief.inputText(TestRoasts.mapper)).path("transcript").values()
            .flatMap { act -> act.path("lines").values().map { line -> line.asString().substringBefore("|").toInt() } }

    /** [brief] 슬롯 수만큼 서로 다른 본인 줄과 칩으로 채운 근거 카드. 점수 깎인 말도 본인 줄을 쓴다. */
    fun evidenceOf(brief: RoastBrief): List<Map<String, Any?>> {
        val lineIds = sentLineIds(brief)
        val chips = brief.facts.keys().filterNot { key -> key == ThanksFactSpec.KEY }
        val kinds = List(brief.slots.reason) { EvidenceKind.REASON } + List(brief.slots.cut) { EvidenceKind.CUT } +
            List(brief.slots.keep) { EvidenceKind.KEEP }
        return kinds.mapIndexed { index, kind ->
            mapOf("kind" to kind.key, "line_id" to lineIds[index], "chip_key" to chips[index], "label" to LABELS[index], "line" to LINES[index])
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
        finalLine: String = "약속 발사대는 올해도 바빴다.",
    ): String = TestRoasts.mapper.writeValueAsString(
        mapOf(
            "verdict_line" to verdictLine,
            "evidence" to evidence,
            "conclusion" to mapOf("title" to title, "hero_line_id" to heroLineId, "hero_chip_key" to heroChipKey, "final_line" to finalLine),
        ),
    )

    fun review(brief: RoastBrief, json: String): RoastReview = RoastReview.of(json, brief, TestRoasts.catalog.rules, TestRoasts.mapper)

    /** 습관이 드러나는 본인 말풍선 150개(다섯 가지 말을 돌려 씀). */
    fun habitMessages() = TestDossiers.selves(
        List(150) { index -> listOf("내일 ㄱ?", "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ", "언제 볼까?", "ㄱㄱ 바로 출발", "넹")[index % 5] },
        minutesApart = 3,
    )
}
