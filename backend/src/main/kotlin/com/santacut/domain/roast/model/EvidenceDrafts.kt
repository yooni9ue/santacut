package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/** 작가 응답의 근거 카드 목록(evidence), 또는 여러 응답에서 통과해 모은 근거 카드. 배열이 아니면 빈 목록이다. */
class EvidenceDrafts(private val values: List<EvidenceDraft>) {
    fun size(): Int = values.size

    /** kind별 개수가 서버 슬롯 수 [slots]와 같은지 본 검증(필드 evidence). */
    fun countCheck(slots: RoastSlots): RoastCheck = RoastCheck(
        FIELD,
        EvidenceKind.entries.mapNotNull { kind ->
            val actual = values.count { draft -> draft.kind() == kind }
            "${kind.key} ${actual}개(${slots.countOf(kind)}개여야 한다)".takeUnless { actual == slots.countOf(kind) }
        } + listOfNotNull("모르는 kind가 있다".takeIf { values.any { draft -> draft.kind() == null } }),
    )

    /** 카드마다의 검증(필드 evidence[0]~). 앞 카드와 같은 line_id·chip_key를 다시 쓰면 떨어진다. */
    fun checks(brief: RoastBrief, rules: RoastRules): List<RoastCheck> = values.mapIndexed { index, draft ->
        val previous = values.take(index)
        RoastCheck(
            fieldOf(index),
            draft.problems(brief, rules) + listOfNotNull(
                "앞 근거와 같은 line_id(${draft.lineId})".takeIf { draft.lineId != null && previous.any { it.lineId == draft.lineId } },
                "앞 근거와 같은 chip_key(${draft.chipKey})".takeIf { previous.any { it.chipKey == draft.chipKey } },
            ),
        )
    }

    /** kind별 개수가 슬롯 수 [slots]와 모두 같은지. */
    fun fills(slots: RoastSlots): Boolean = EvidenceKind.entries.all { kind -> countOf(kind) == slots.countOf(kind) }

    /**
     * 이 카드들에 응답 근거 카드 [response] 가운데 카드 검증 [checks]를 통과한 것을 더한다. 한 장이 떨어져도 나머지는 남긴다.
     * kind별 슬롯 수 [slots]를 넘거나 이미 가진 카드와 line_id·chip_key가 겹치는 카드는 더하지 않는다.
     * 다시 쓰기 응답이 이미 받은 카드를 그대로 다시 내도 겹침으로 걸러지고, 떨어졌던 자리만 새 카드로 찬다.
     */
    fun absorbed(response: EvidenceDrafts, checks: RoastChecks, slots: RoastSlots): EvidenceDrafts =
        response.values.withIndex()
            .filter { (index, _) -> checks.passed(fieldOf(index)) }
            .fold(this) { kept, (_, draft) -> if (kept.accepts(draft, slots)) EvidenceDrafts(kept.values + draft) else kept }

    private fun accepts(draft: EvidenceDraft, slots: RoastSlots): Boolean {
        val kind = draft.kind() ?: return false
        return countOf(kind) < slots.countOf(kind) &&
            values.none { kept -> kept.chipKey == draft.chipKey || (draft.lineId != null && kept.lineId == draft.lineId) }
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
