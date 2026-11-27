package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/** 작가 응답 JSON(작가 프롬프트 5판 스키마)을 읽은 값. 빠진 필드는 빈 문자열·빈 목록이라 검증에서 떨어진다. */
class RoastOutput(
    val verdictLine: String,
    val evidence: EvidenceDrafts,
    val conclusion: ConclusionDraft,
) {
    /** 응답 전체에 쓴 허용 유행어 수. 덱 전체 상한을 넘었는지 볼 때 쓴다. */
    fun slangCount(rules: RoastRules): Int = rules.slangCountIn(verdictLine) + evidence.slangCount(rules) + conclusion.slangCount(rules)

    companion object {
        private const val VERDICT_LINE = "verdict_line"
        private const val EVIDENCE = "evidence"
        private const val CONCLUSION = "conclusion"

        fun from(node: JsonNode): RoastOutput = RoastOutput(
            RoastJson.textOf(node, VERDICT_LINE), EvidenceDrafts.from(node.path(EVIDENCE)), ConclusionDraft.from(node.path(CONCLUSION)),
        )
    }
}
