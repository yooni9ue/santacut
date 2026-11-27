package com.santacut.domain.roast.model

/**
 * 작가 응답의 근거 카드 하나. 빠진 문자열은 빈 문자열, 빠졌거나 정수가 아닌 줄 번호는 null로 읽어 검증에서 떨어지게 한다.
 *
 * @property kindKey 근거 종류 글자(reason, cut, keep)
 * @property lineId 고른 본인 줄 번호. 점수 깎인 말만 null이어도 된다.
 * @property chipKey 고른 칩 후보 key
 */
data class EvidenceDraft(val kindKey: String, val lineId: Int?, val chipKey: String, val label: String, val line: String) {
    fun kind(): EvidenceKind? = EvidenceKind.ofKey(kindKey)

    /**
     * 문장 검증과 본인 줄·칩 후보 확인. 칩은 방향이 맞는 종류에만 쓴다([ChipDirection.allows]): 이유·살아남은 말은 좋은 쪽, 깎인 말은 나쁜 쪽(고마워 칩 포함),
     * 중립 칩(달별 기복, 근무시간 비율 등)은 어느 근거에도 못 쓴다. 한 줄은 고른 말풍선이나 칩(이름, 뜻 풀이, 버릇 낱말)과 낱말이 겹쳐야 하고([TextProblems.grounding]),
     * 지표 칩 이름을 그대로 옮기거나([TextProblems.restated]) 칩·말풍선에 없는 시간대를 말하면([TextProblems.timeClaims]) 떨어진다.
     * 말풍선으로 보여 줄 수 없는 지표 칩([QuoteRule.NONE])은 어느 종류 카드든 말풍선 없이(line_id null) 칩만 크게 띄운다.
     * 앞 카드와 겹치는지는 [EvidenceDrafts]가 본다. 말풍선 검사는 [quoteProblems]다.
     */
    fun problems(brief: RoastBrief, rules: RoastRules): List<String> {
        val kind = kind()
        val quoteless = brief.quoteRuleOf(chipKey) == QuoteRule.NONE
        val quote = lineId?.let(brief::quoteOf)
        return quoteProblems(brief, rules) + listOfNotNull(
            "모르는 kind($kindKey)".takeIf { kind == null },
            "line_id가 없다".takeIf { lineId == null && kind?.lineOptional == false && !quoteless },
            "보낸 본인 줄이 아닌 line_id($lineId)".takeIf { lineId != null && quote == null },
            "없는 chip_key($chipKey)".takeUnless { brief.hasFact(chipKey) },
            directionProblem(brief, kind),
            "이 칩($chipKey)은 말풍선 없이 쓴다(line_id를 null로)".takeIf { quoteless && lineId != null && brief.hasFact(chipKey) },
        ) + TextProblems.of(label, RoastTextKind.LABEL, brief, rules).labeled(LABEL) +
            TextProblems.copiedExample(label, brief).labeled(LABEL) + TextProblems.copiedExample(line, brief).labeled(LINE) +
            TextProblems.copiedLabel(label, rules.labelSuffix, quote).labeled(LABEL) +
            TextProblems.of(line, RoastTextKind.EVIDENCE_LINE, brief, rules).labeled(LINE) +
            TextProblems.grounding(line, quote, brief.chipTextsOf(chipKey)).labeled(LINE) +
            TextProblems.restated(line, brief.chipOf(chipKey)?.label?.takeIf { brief.isRestatable(chipKey) }).labeled(LINE) +
            TextProblems.timeClaims(line, rules, brief.chipTimeTextsOf(chipKey), quote).labeled(LINE)
    }

    /** 칩 방향이 이 카드 종류와 맞지 않는 사유. 칩 후보가 아니거나 종류를 모르면 보지 않는다. */
    private fun directionProblem(brief: RoastBrief, kind: EvidenceKind?): String? {
        val direction = brief.directionOf(chipKey) ?: return null
        if (kind == null || direction.allows(kind)) return null
        val allowed = direction.kindKeys().orEmpty()
        return "이 칩($chipKey)은 ${kind.key}에 쓰지 않는다(쓸 수 있는 kind: ${allowed.ifEmpty { listOf(NO_KIND) }.joinToString(", ")})"
    }

    /**
     * 고른 말풍선 검사. ㅇㅇ·ㅋㅋ 같은 반응이나 한글이 세 음절 이하인 단답([QuoteText.isSubstantial])은 버릇을 보여 주지 못해 떨어진다.
     * 말풍선은 칩의 말풍선 조건([RoastBrief.quoteFits])에 맞아야 한다. 입버릇 칩이면 그 입버릇이 있어야 하고(칩 횟수와 같은 표면형),
     * 물음표 칩이면 물음표가, 따뜻한 말 칩이면 따뜻한 말이 든 줄이어야 한다. 칩과 말풍선이 다른 이야기를 하지 않게 한다('언제 가능?'에 피크타임 칩).
     * 선물 받는 이유·살아남은 말에는 칭찬을 뒤집는 말('수고했다 근데 별로임'의 근데)이 든 말풍선을 쓰지 않는다.
     */
    private fun quoteProblems(brief: RoastBrief, rules: RoastRules): List<String> {
        val quote = lineId?.let(brief::quoteOf) ?: return emptyList()
        val praising = kind() == EvidenceKind.REASON || kind() == EvidenceKind.KEEP
        val rule = brief.quoteRuleOf(chipKey)
        val phrase = brief.phraseOf(chipKey)
        val mismatched = brief.hasFact(chipKey) && rule != QuoteRule.NONE && !brief.quoteFits(chipKey, lineId)
        return listOfNotNull(
            "말풍선이 너무 짧다(한글 ${QuoteText.MIN_SYLLABLES}음절 이상인 줄만 고른다)".takeUnless { QuoteText.isSubstantial(quote) },
            "말풍선에 칩 입버릇('$phrase')이 없다".takeIf { phrase != null && mismatched },
            "말풍선이 칩과 맞지 않는다(${rule.hint})".takeIf { phrase == null && mismatched },
            rules.flipIn(quote)?.takeIf { praising }?.let { flip -> "칭찬을 뒤집는 말풍선($flip)은 reason·keep 근거로 쓰지 않는다" },
        )
    }

    /** 다시 쓰기 입력에 싣는 받아 둔 카드. */
    fun toKept(): KeptCard = KeptCard(kindKey, lineId, chipKey, label, line)

    /** 검증을 통과한 이 카드를 저장할 근거로 바꾼다. 말풍선과 칩은 [brief]에서 서버가 채운다. */
    fun toEvidence(brief: RoastBrief): RoastEvidence? {
        val kind = kind() ?: return null
        return RoastEvidence(
            kind, lineId, chipKey, brief.chipOf(chipKey), lineId?.let(brief::quoteOf), label, line, RoastSource.AI, brief.dimensionOf(chipKey),
        )
    }

    companion object {
        private const val LABEL = "label"
        private const val LINE = "line"
        private const val NO_KIND = "없음, hero_chip_key로만"
    }
}
