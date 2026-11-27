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
     * 문장 검증과 본인 줄·칩 후보 확인. 고마워 칩은 점수 깎인 말에만 쓴다. 한 줄은 고른 말풍선이나 칩(이름, 뜻 풀이)과 낱말이 겹쳐야 한다([TextProblems.grounding]).
     * 앞 카드와 겹치는지는 [EvidenceDrafts]가 본다. 말풍선 검사는 [quoteProblems]다.
     */
    fun problems(brief: RoastBrief, rules: RoastRules): List<String> {
        val kind = kind()
        return quoteProblems(brief) + listOfNotNull(
            "모르는 kind($kindKey)".takeIf { kind == null },
            "line_id가 없다".takeIf { lineId == null && kind?.lineOptional == false },
            "보낸 본인 줄이 아닌 line_id($lineId)".takeIf { lineId != null && brief.quoteOf(lineId) == null },
            "없는 chip_key($chipKey)".takeUnless { brief.hasFact(chipKey) },
            "고마워 칩(${ThanksFactSpec.KEY})은 cut에만 쓴다".takeIf { chipKey == ThanksFactSpec.KEY && kind != null && kind != EvidenceKind.CUT },
        ) + TextProblems.of(label, RoastTextKind.LABEL, brief, rules).labeled(LABEL) +
            TextProblems.of(line, RoastTextKind.EVIDENCE_LINE, brief, rules).labeled(LINE) +
            TextProblems.grounding(line, lineId?.let(brief::quoteOf), brief.chipTextsOf(chipKey)).labeled(LINE)
    }

    /**
     * 고른 말풍선 검사. ㅇㅇ·ㅋㅋ 같은 반응이나 한글이 세 음절 이하인 단답([QuoteText.isSubstantial])은 버릇을 보여 주지 못해 떨어진다.
     * 입버릇 칩을 골랐으면 말풍선에 그 입버릇이 있어야 한다([RoastBrief.quoteHasPhrase], 칩 횟수와 같은 표면형으로 센다). 칩과 말풍선이 다른 이야기를 하지 않게 한다.
     */
    private fun quoteProblems(brief: RoastBrief): List<String> {
        val quote = lineId?.let(brief::quoteOf) ?: return emptyList()
        val phrase = brief.phraseOf(chipKey)
        return listOfNotNull(
            "말풍선이 너무 짧다(한글 ${QuoteText.MIN_SYLLABLES}음절 이상인 줄만 고른다)".takeUnless { QuoteText.isSubstantial(quote) },
            "말풍선에 칩 입버릇('$phrase')이 없다".takeIf { phrase != null && !brief.quoteHasPhrase(chipKey, quote) },
        )
    }

    /** 검증을 통과한 이 카드를 저장할 근거로 바꾼다. 말풍선과 칩은 [brief]에서 서버가 채운다. */
    fun toEvidence(brief: RoastBrief): RoastEvidence? {
        val kind = kind() ?: return null
        return RoastEvidence(kind, lineId, chipKey, brief.chipOf(chipKey), lineId?.let(brief::quoteOf), label, line, RoastSource.AI)
    }

    companion object {
        private const val LABEL = "label"
        private const val LINE = "line"
    }
}
