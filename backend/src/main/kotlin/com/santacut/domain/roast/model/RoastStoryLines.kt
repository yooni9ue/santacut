package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.databind.JsonNode

/**
 * 세 막 줄거리. 저장할 때는 막마다 한 줄씩 막 순서다(12/9 화면에 쓰려고 지금부터 저장한다). JSON에서는 배열 하나로 읽고 쓴다.
 */
class RoastStoryLines @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<RoastStoryLine>,
) {
    @JsonValue
    fun toList(): List<RoastStoryLine> = values.toList()

    fun size(): Int = values.size

    fun fallbackCount(): Int = values.count { line -> line.source == RoastSource.FALLBACK }

    /** 줄마다의 검증. 문장 검증에 더해 막 번호가 1~3이고 앞 줄과 겹치지 않는지 본다. */
    fun checks(brief: RoastBrief, rules: RoastRules): List<RoastCheck> = values.mapIndexed { index, line ->
        val duplicated = values.take(index).any { previous -> previous.act == line.act }
        RoastCheck(
            fieldOf(index),
            listOfNotNull(
                "막 번호가 1~${RoastSheet.ACTS}이 아니다".takeUnless { line.act in 1..RoastSheet.ACTS },
                "앞 줄과 같은 막".takeIf { duplicated },
            ) + TextProblems.of(line.line, RoastTextKind.STORY, brief, rules).toList(),
        )
    }

    fun passing(checks: RoastChecks): RoastStoryLines = RoastStoryLines(values.filterIndexed { index, _ -> checks.passed(fieldOf(index)) })

    /** 지금 줄에 [other]의 줄을 막이 겹치지 않게 더한다. 막 순서로 둔다. */
    fun plus(other: RoastStoryLines): RoastStoryLines =
        RoastStoryLines((values + other.values).distinctBy(RoastStoryLine::act).sortedBy(RoastStoryLine::act))

    /** 빠진 막을 대체 줄거리로 채운다. */
    fun completedWith(fallback: RoastFallback, seed: Int): RoastStoryLines {
        val written = values.map(RoastStoryLine::act)
        val missing = (1..RoastSheet.ACTS).filterNot { act -> act in written }.map { act -> fallback.storyLine(act, seed) }
        return RoastStoryLines((values + missing).sortedBy(RoastStoryLine::act))
    }

    companion object {
        val EMPTY = RoastStoryLines(emptyList())
        const val FIELD = "story"
        private const val ACT = "act"
        private const val LINE = "line"

        fun fieldOf(index: Int): String = "$FIELD[$index]"

        /** 작가 응답의 story 배열. 막 번호가 정수가 아니면 0으로 읽어 검증에서 떨어지게 한다. */
        fun from(node: JsonNode): RoastStoryLines = RoastStoryLines(
            node.values().map { item -> RoastStoryLine(RoastJson.intOf(item, ACT) ?: 0, RoastJson.textOf(item, LINE)) },
        )
    }
}
