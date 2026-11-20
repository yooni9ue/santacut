package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/** 작가 응답 JSON을 읽은 값. 빠진 필드는 빈 문자열·빈 목록이라 검증에서 떨어진다. */
class RoastOutput(
    val title: String,
    val verdict: String,
    val verdictMild: String,
    val pairs: RoastPairs,
    val letter: String,
    val story: RoastStoryLines,
) {
    companion object {
        private const val TITLE = "title"
        private const val VERDICT = "verdict"
        private const val VERDICT_MILD = "verdict_mild"
        private const val PAIRS = "pairs"
        private const val LETTER = "letter"
        private const val STORY = "story"

        fun from(node: JsonNode): RoastOutput = RoastOutput(
            RoastJson.textOf(node, TITLE), RoastJson.textOf(node, VERDICT), RoastJson.textOf(node, VERDICT_MILD),
            RoastPairs.from(node.path(PAIRS)), RoastJson.textOf(node, LETTER), RoastStoryLines.from(node.path(STORY)),
        )
    }
}
