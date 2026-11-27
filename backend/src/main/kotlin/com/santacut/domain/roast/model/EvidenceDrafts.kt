package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/** 작가 응답의 근거 카드 목록(evidence), 또는 여러 응답에서 통과해 모은 근거 카드. 배열이 아니면 빈 목록이다. */
class EvidenceDrafts(private val values: List<EvidenceDraft>) {
    fun size(): Int = values.size

    /** 고른 본인 줄 번호(말풍선 없는 카드는 뺀다). */
    fun lineIds(): Set<Int> = values.mapNotNull(EvidenceDraft::lineId).toSet()

    /** kind별 개수가 서버 슬롯 수 [slots]와 같은지 본 검증(필드 evidence). */
    fun countCheck(slots: RoastSlots): RoastCheck = RoastCheck(
        FIELD,
        EvidenceKind.entries.mapNotNull { kind ->
            val actual = values.count { draft -> draft.kind() == kind }
            "${kind.key} ${actual}개(${slots.countOf(kind)}개여야 한다)".takeUnless { actual == slots.countOf(kind) }
        } + listOfNotNull("모르는 kind가 있다".takeIf { values.any { draft -> draft.kind() == null } }),
    )

    /**
     * 카드마다의 검증(필드 evidence[0]~). 앞 카드와 같은 line_id·chip_key를 다시 쓰거나, 앞 카드 한 줄의 낱말을 되풀이하거나,
     * 라벨 첫머리가 앞 라벨과 같거나 판정 한 줄 [verdictLine]을 옮긴 라벨이면 떨어진다.
     */
    fun checks(brief: RoastBrief, rules: RoastRules, verdictLine: String = ""): List<RoastCheck> = values.mapIndexed { index, draft ->
        val previous = values.take(index)
        RoastCheck(
            fieldOf(index),
            draft.problems(brief, rules) + listOfNotNull(
                "앞 근거와 같은 line_id(${draft.lineId})".takeIf { draft.lineId != null && previous.any { it.lineId == draft.lineId } },
                "앞 근거와 같은 chip_key(${draft.chipKey})".takeIf { previous.any { it.chipKey == draft.chipKey } },
            ) + EvidenceDrafts(previous).overlapsOf(draft, brief, rules, verdictLine),
        )
    }

    /** [draft]가 이 카드들과 겹치는 문장 사유(한 줄 낱말 되풀이, 라벨 첫머리, 판정 한 줄을 옮긴 라벨). 없으면 빈 목록이다. */
    private fun overlapsOf(draft: EvidenceDraft, brief: RoastBrief, rules: RoastRules, verdictLine: String): List<String> =
        TextProblems.repeated(draft.line, values.map(EvidenceDraft::line), brief.chipTextsOf(draft.chipKey)).labeled(LINE) +
            TextProblems.sharedLabel(draft.label, values.map(EvidenceDraft::label), verdictLine, rules.labelSuffix).labeled(LABEL)

    /** kind별 개수가 슬롯 수 [slots]와 모두 같은지. */
    fun fills(slots: RoastSlots): Boolean = EvidenceKind.entries.all { kind -> countOf(kind) == slots.countOf(kind) }

    /**
     * 이 카드들에 응답 근거 카드 [response] 가운데 카드 검증 [checks]를 통과한 것을 더한다. 한 장이 떨어져도 나머지는 남긴다.
     * kind별 슬롯 수([RoastBrief.slots])를 넘거나 이미 가진 카드와 line_id·chip_key·문장이 겹치는 카드는 더하지 않는다.
     * 다시 쓰기 응답이 이미 받은 카드를 그대로 다시 내도 겹침으로 걸러지고, 떨어졌던 자리만 새 카드로 찬다.
     */
    fun absorbed(response: EvidenceDrafts, checks: RoastChecks, brief: RoastBrief, rules: RoastRules): EvidenceDrafts =
        response.values.withIndex()
            .filter { (index, _) -> checks.passed(fieldOf(index)) }
            .fold(this) { kept, (_, draft) -> if (kept.accepts(draft, brief, rules)) EvidenceDrafts(kept.values + draft) else kept }

    /** 슬롯이 남았고, 이미 가진 카드와 line_id·chip_key·문장이 겹치지 않는 카드만 받는다(다른 응답에서 온 카드끼리도 덱 안 되풀이를 막는다). */
    private fun accepts(draft: EvidenceDraft, brief: RoastBrief, rules: RoastRules): Boolean {
        val kind = draft.kind() ?: return false
        return countOf(kind) < brief.slots.countOf(kind) &&
            values.none { kept -> kept.chipKey == draft.chipKey || (draft.lineId != null && kept.lineId == draft.lineId) } &&
            overlapsOf(draft, brief, rules, "").isEmpty()
    }

    private fun countOf(kind: EvidenceKind): Int = values.count { draft -> draft.kind() == kind }

    /** 문장에 쓴 허용 유행어 수. */
    fun slangCount(rules: RoastRules): Int = values.sumOf { draft -> rules.slangCountIn(draft.label) + rules.slangCountIn(draft.line) }

    /** 검증을 통과한 카드를 저장할 근거로 바꾼다. 덱 순서(이유, 깎인 말, 살아남은 말)로 줄 세운다. */
    fun toEvidences(brief: RoastBrief): RoastEvidences =
        RoastEvidences(values.mapNotNull { draft -> draft.toEvidence(brief) }.sortedBy { evidence -> evidence.kind.ordinal })

    companion object {
        val EMPTY = EvidenceDrafts(emptyList())
        const val FIELD = "evidence"
        private const val KIND = "kind"
        private const val LINE_ID = "line_id"
        private const val CHIP_KEY = "chip_key"
        private const val LABEL = "label"
        private const val LINE = "line"

        fun fieldOf(index: Int): String = "$FIELD[$index]"

        fun from(node: JsonNode): EvidenceDrafts = EvidenceDrafts(
            node.values().map { item ->
                EvidenceDraft(
                    RoastJson.textOf(item, KIND), RoastJson.intOf(item, LINE_ID), RoastJson.textOf(item, CHIP_KEY),
                    RoastJson.textOf(item, LABEL), RoastJson.textOf(item, LINE),
                )
            },
        )
    }
}
