package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/** 작가 응답의 근거 카드 목록(evidence). 배열이 아니면 빈 목록이다. */
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

    /** 문장에 쓴 허용 유행어 수. */
    fun slangCount(rules: RoastRules): Int = values.sumOf { draft -> rules.slangCountIn(draft.label) + rules.slangCountIn(draft.line) }

    /** 검증을 통과한 카드를 저장할 근거로 바꾼다. 덱 순서(이유, 깎인 말, 살아남은 말)로 줄 세운다. */
    fun toEvidences(brief: RoastBrief): RoastEvidences =
        RoastEvidences(values.mapNotNull { draft -> draft.toEvidence(brief) }.sortedBy { evidence -> evidence.kind.ordinal })

    companion object {
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
