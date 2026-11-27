package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/**
 * 작가 응답의 결론 카드(conclusion). 빠진 문자열은 빈 문자열, 빠진 번호·key는 null로 읽는다.
 * 대표 말과 칩은 근거 카드에서 쓴 것을 다시 써도 된다.
 */
data class ConclusionDraft(val title: String, val heroLineId: Int?, val heroChipKey: String?, val finalLine: String) {
    /**
     * 결론 검증(필드 conclusion). 대표 줄은 보낸 본인 줄이어야 하고 대표 칩은 고마워 칩이 아닌 칩 후보여야 한다. 보낸 줄·후보가 없을 때만 비워도 된다.
     * 마지막 한 줄은 대표 말풍선이나 대표 칩(이름, 뜻 풀이)과 낱말이 겹쳐야 한다([TextProblems.grounding]).
     */
    fun check(brief: RoastBrief, rules: RoastRules): RoastCheck = RoastCheck(
        FIELD,
        listOfNotNull(heroLineProblem(brief), heroChipProblem(brief)) +
            TextProblems.of(title, RoastTextKind.TITLE, brief, rules).labeled(TITLE) +
            TextProblems.of(finalLine, RoastTextKind.FINAL_LINE, brief, rules).labeled(FINAL_LINE) +
            TextProblems.grounding(finalLine, heroLineId?.let(brief::quoteOf), heroChipKey?.let(brief::chipTextsOf).orEmpty())
                .labeled(FINAL_LINE),
    )

    fun slangCount(rules: RoastRules): Int = rules.slangCountIn(title) + rules.slangCountIn(finalLine)

    /** 검증을 통과한 결론을 저장할 값으로 바꾼다. 대표 말풍선과 칩은 [brief]에서 서버가 채운다. */
    fun toConclusion(brief: RoastBrief): RoastConclusion = RoastConclusion(
        RoastText(title, RoastSource.AI), heroLineId, heroChipKey, heroLineId?.let(brief::quoteOf), heroChipKey?.let(brief::chipOf),
        RoastText(finalLine, RoastSource.AI),
    )

    private fun heroLineProblem(brief: RoastBrief): String? = when {
        heroLineId == null -> "hero_line_id가 없다".takeIf { brief.lineCount() > 0 }
        brief.quoteOf(heroLineId) == null -> "보낸 본인 줄이 아닌 hero_line_id($heroLineId)"
        else -> null
    }

    private fun heroChipProblem(brief: RoastBrief): String? = when {
        heroChipKey == null -> "hero_chip_key가 없다".takeIf { brief.facts.size() > 0 }
        !brief.hasFact(heroChipKey) -> "없는 hero_chip_key($heroChipKey)"
        heroChipKey == ThanksFactSpec.KEY -> "고마워 칩(${ThanksFactSpec.KEY})은 hero_chip_key로 쓰지 않는다"
        else -> null
    }

    companion object {
        const val FIELD = "conclusion"
        private const val TITLE = "title"
        private const val HERO_LINE_ID = "hero_line_id"
        private const val HERO_CHIP_KEY = "hero_chip_key"
        private const val FINAL_LINE = "final_line"

        fun from(node: JsonNode): ConclusionDraft = ConclusionDraft(
            RoastJson.textOf(node, TITLE), RoastJson.intOf(node, HERO_LINE_ID),
            RoastJson.textOf(node, HERO_CHIP_KEY).ifEmpty { null }, RoastJson.textOf(node, FINAL_LINE),
        )
    }
}
