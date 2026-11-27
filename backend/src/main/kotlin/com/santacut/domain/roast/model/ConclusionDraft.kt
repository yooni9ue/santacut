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
     * 판정 한 줄 [verdictLine]과 같은 유행어나 같은 펀치 틀을 쓰면 떨어진다. 대표 말풍선은 대표 칩의 말풍선 조건에 맞아야 하고,
     * 말풍선으로 보여 줄 수 없는 칩([QuoteRule.NONE])을 대표 칩으로 고르면 대표 말 없이(hero_line_id null) 칩만 크게 띄운다.
     * 근거 카드 [evidence]가 [RoastEvidences.MIN_CARDS]장뿐인 다섯 장 덱이면 대표 말을 근거 카드 말과 다른 줄로 골라야 한다(다른 줄이 남아 있을 때).
     * 짧은 덱에서 같은 말풍선이 두 번 뜨지 않게 한다. 대표 사실 칩([RoastBrief.signatureKey])은 근거 카드나 대표 칩 가운데 한 곳에 써야 하고,
     * 칭호는 근거 카드 라벨과 덱 되풀이 금지 줄기를 함께 쓰지 않으며, 마지막 한 줄은 대표 칩·대표 말풍선에 없는 시간대를 말하지 않는다.
     */
    fun check(
        brief: RoastBrief,
        rules: RoastRules,
        verdictLine: String = "",
        evidence: EvidenceDrafts = EvidenceDrafts.EMPTY,
        kept: EvidenceDrafts = EvidenceDrafts.EMPTY,
    ): RoastCheck = RoastCheck(
        FIELD,
        listOfNotNull(
            heroLineProblem(brief),
            heroChipProblem(brief),
            heroRepeatProblem(brief, evidence),
            heroFitProblem(brief),
            rules.punchClashIn(verdictLine, finalLine)?.let { mark -> "$FINAL_LINE: 판정 한 줄과 같은 펀치($mark)를 되풀이했다" },
            signatureProblem(brief, evidence, kept),
        ) +
            TextProblems.repeatedStem(title, evidence.labels(), rules).labeled(TITLE) +
            TextProblems.copiedExample(title, brief).labeled(TITLE) + TextProblems.copiedExample(finalLine, brief).labeled(FINAL_LINE) +
            TextProblems.timeClaims(finalLine, rules, heroChipKey?.let(brief::chipTimeTextsOf).orEmpty(), heroLineId?.let(brief::quoteOf))
                .labeled(FINAL_LINE) +
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
        heroLineId == null -> "hero_line_id가 없다".takeIf { brief.lineCount() > 0 && !isQuotelessHero(brief) }
        brief.quoteOf(heroLineId) == null -> "보낸 본인 줄이 아닌 hero_line_id($heroLineId)"
        else -> null
    }

    /**
     * 대표 말풍선과 대표 칩은 결론 카드에 나란히 뜨므로 칩의 말풍선 조건([RoastBrief.quoteFits])에 맞아야 한다.
     * 말풍선 없이 쓰는 칩([QuoteRule.NONE])이면 대표 말을 비워야 한다.
     */
    private fun heroFitProblem(brief: RoastBrief): String? {
        if (heroLineId == null || heroChipKey == null || brief.quoteOf(heroLineId) == null || !brief.hasFact(heroChipKey)) return null
        if (isQuotelessHero(brief)) return "말풍선 없이 쓰는 대표 칩($heroChipKey)이면 hero_line_id를 null로 둔다"
        val rule = brief.quoteRuleOf(heroChipKey)
        return "대표 말풍선이 대표 칩과 맞지 않는다(${rule.hint})".takeUnless { brief.quoteFits(heroChipKey, heroLineId) }
    }

    /** 대표 칩이 말풍선으로 보여 줄 수 없는 칩 후보인지. 고마워 칩은 대표 칩이 될 수 없어 빼고 본다. */
    private fun isQuotelessHero(brief: RoastBrief): Boolean =
        heroChipKey != null && heroChipKey != ThanksFactSpec.KEY && brief.hasFact(heroChipKey) && brief.quoteRuleOf(heroChipKey) == QuoteRule.NONE

    private fun heroRepeatProblem(brief: RoastBrief, evidence: EvidenceDrafts): String? {
        val lineIds = evidence.lineIds()
        val short = brief.slots.total() <= RoastEvidences.MIN_CARDS && brief.usableLineCount() > lineIds.size
        return "다섯 장 덱이라 hero_line_id($heroLineId)는 근거 카드와 다른 줄로 고른다".takeIf { short && heroLineId != null && heroLineId in lineIds }
    }

    /**
     * 대표 사실 칩을 이 응답의 근거 카드 [evidence], 앞 응답에서 받아 둔 근거 카드 [kept], 대표 칩 어디에도 쓰지 않은 사유. 대표 사실이 없으면 보지 않는다.
     * 다시 쓰기 응답이 결론만 고쳐 낼 때 받아 둔 카드가 대표 사실을 이미 썼으면 통과한다.
     */
    private fun signatureProblem(brief: RoastBrief, evidence: EvidenceDrafts, kept: EvidenceDrafts): String? {
        val signature = brief.signatureKey() ?: return null
        if (signature == heroChipKey || signature in evidence.chipKeys() || signature in kept.chipKeys()) return null
        return "대표 사실 칩($signature)을 근거 카드나 hero_chip_key 가운데 한 곳에 쓴다"
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
