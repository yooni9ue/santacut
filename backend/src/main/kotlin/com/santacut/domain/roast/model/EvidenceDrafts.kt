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
     * 앞 카드는 제 검증([EvidenceDraft.problems])을 통과한 카드만 견준다. 떨어질 카드 때문에 멀쩡한 뒤 카드까지 떨어지지 않게 한다.
     */
    fun checks(brief: RoastBrief, rules: RoastRules, verdictLine: String = ""): List<RoastCheck> {
        val problems = values.map { draft -> draft.problems(brief, rules) }
        return values.mapIndexed { index, draft ->
            val previous = values.take(index).filterIndexed { earlier, _ -> problems[earlier].isEmpty() }
            RoastCheck(fieldOf(index), problems[index] + EvidenceDrafts(previous).duplicatesOf(draft, brief, rules, verdictLine))
        }
    }

    /** kind별 개수가 슬롯 수 [slots]와 모두 같은지. */
    fun fills(slots: RoastSlots): Boolean = EvidenceKind.entries.all { kind -> countOf(kind) == slots.countOf(kind) }

    /**
     * 이 카드들에 응답 근거 카드 [response] 가운데 카드 검증 [checks]를 통과한 것을 더한다. 한 장이 떨어져도 나머지는 남긴다.
     * kind별 슬롯 수([RoastBrief.slots])를 넘거나 이미 받은 카드와 line_id·chip_key·문장이 겹치는 카드는 더하지 않고, 그 사유를 결과에 남긴다.
     * 다시 쓰기 응답이 이미 받은 카드를 그대로 다시 내면 사유 없이 건너뛰고, 떨어졌던 자리만 새 카드로 찬다.
     */
    fun absorbed(response: EvidenceDrafts, checks: RoastChecks, brief: RoastBrief, rules: RoastRules): EvidenceIntake =
        response.values.withIndex()
            .filter { (index, _) -> checks.passed(fieldOf(index)) }
            .fold(EvidenceIntake(this, emptyList())) { intake, (index, draft) ->
                val reasons = intake.kept.rejectionsOf(draft, brief, rules)
                when {
                    reasons.isEmpty() -> EvidenceIntake(EvidenceDrafts(intake.kept.values + draft), intake.reasons)
                    draft in intake.kept.values -> intake
                    else -> EvidenceIntake(intake.kept, intake.reasons + reasons.map { reason -> "${fieldOf(index)} $KEPT_PREFIX$reason" })
                }
            }

    /** 카드 라벨. 결론 칭호가 라벨과 같은 줄기를 되풀이하는지 볼 때 쓴다. */
    fun labels(): List<String> = values.map(EvidenceDraft::label)

    /** 쓴 칩 key. 대표 사실 칩을 썼는지 볼 때 쓴다. */
    fun chipKeys(): Set<String> = values.map(EvidenceDraft::chipKey).toSet()

    /** 다시 쓰기 입력에 싣는 받아 둔 카드. */
    fun toKept(): List<KeptCard> = values.map(EvidenceDraft::toKept)

    /** 문장에 쓴 허용 유행어 수. */
    fun slangCount(rules: RoastRules): Int = values.sumOf { draft -> rules.slangCountIn(draft.label) + rules.slangCountIn(draft.line) }

    /** 검증을 통과한 카드를 저장할 근거로 바꾼다. 덱 순서(이유, 깎인 말, 살아남은 말)로 줄 세운다. */
    fun toEvidences(brief: RoastBrief): RoastEvidences =
        RoastEvidences(values.mapNotNull { draft -> draft.toEvidence(brief) }.sortedBy { evidence -> evidence.kind.ordinal })

    /**
     * [draft]를 받지 않는 사유. 슬롯이 찼거나, 이미 받은 카드와 line_id·chip_key·문장이 겹치거나, 본인 말 입버릇 카드 몫으로 남긴 마지막 깎인 말 자리를
     * 다른 카드로 채우려 하면 그 사유이고, 받을 수 있으면 빈 목록이다.
     */
    private fun rejectionsOf(draft: EvidenceDraft, brief: RoastBrief, rules: RoastRules): List<String> {
        val kind = draft.kind() ?: return listOf("모르는 kind(${draft.kindKey})")
        val full = "${kind.key} 슬롯이 이미 찼다".takeUnless { countOf(kind) < brief.slots.countOf(kind) }
        return listOfNotNull(full, quoteCutProblem(draft, kind, brief)) + duplicatesOf(draft, brief, rules, "")
    }

    /**
     * 마지막 깎인 말 자리를 본인 말 입버릇 카드([RoastBrief.quoteCutKey])가 아닌 카드로 채우려 하는 사유. 받아 둔 깎인 말에 그런 카드가 이미 있거나,
     * 그 입버릇 칩을 다른 카드가 이미 썼거나, 깎인 말이 아니면 보지 않는다. 석탄 덱 깎인 말 세 장이 모두 말풍선 없는 지표 칩으로 차던 문제를 막는다.
     */
    private fun quoteCutProblem(draft: EvidenceDraft, kind: EvidenceKind, brief: RoastBrief): String? {
        if (kind != EvidenceKind.CUT || draft.isQuotedPhraseCut() || values.any(EvidenceDraft::isQuotedPhraseCut)) return null
        if (countOf(EvidenceKind.CUT) + 1 < brief.slots.cut) return null
        val key = brief.quoteCutKey()?.takeIf { key -> key !in chipKeys() } ?: return null
        return "마지막 cut 자리는 본인 말풍선을 붙인 입버릇 칩($key, line_id 필수)으로 채운다"
    }

    /**
     * [draft]가 이 카드들과 겹치는 사유(같은 line_id·chip_key, 깎인 말과 이유·살아남은 말이 같은 버릇, 낱말 되풀이, 라벨 첫머리, 라벨 되풀이 줄기,
     * 판정 한 줄을 옮긴 라벨). 없으면 빈 목록이다.
     */
    private fun duplicatesOf(draft: EvidenceDraft, brief: RoastBrief, rules: RoastRules, verdictLine: String): List<String> =
        listOfNotNull(
            "앞 근거와 같은 line_id(${draft.lineId})".takeIf { draft.lineId != null && values.any { kept -> kept.lineId == draft.lineId } },
            "앞 근거와 같은 chip_key(${draft.chipKey})".takeIf { values.any { kept -> kept.chipKey == draft.chipKey } },
            oppositeTopicProblem(draft, brief),
        ) + TextProblems.repeated(draft.line, values.map(EvidenceDraft::line), brief.chipTextsOf(draft.chipKey)).labeled(LINE) +
            TextProblems.sharedLabel(draft.label, values.map(EvidenceDraft::label), verdictLine, rules.labelSuffix).labeled(LABEL) +
            TextProblems.repeatedStem(draft.label, values.map(EvidenceDraft::label), rules).labeled(LABEL)

    private fun countOf(kind: EvidenceKind): Int = values.count { draft -> draft.kind() == kind }

    /** [draft]가 반대편 카드(깎인 말 ↔ 이유·살아남은 말)와 같은 버릇([RoastBrief.topicOf])을 말하는 사유. 깎은 버릇을 살아남은 말로 다시 띄우지 않게 한다. */
    private fun oppositeTopicProblem(draft: EvidenceDraft, brief: RoastBrief): String? {
        val cut = draft.isCut() ?: return null
        val topic = brief.topicOf(draft.chipKey)
        val clash = values.any { kept -> kept.isCut()?.let { keptCut -> keptCut != cut } == true && brief.topicOf(kept.chipKey) == topic }
        return "깎인 말과 이유·살아남은 말이 같은 버릇($topic)을 쓴다".takeIf { clash }
    }

    companion object {
        val EMPTY = EvidenceDrafts(emptyList())
        const val FIELD = "evidence"
        private const val KIND = "kind"
        private const val LINE_ID = "line_id"
        private const val CHIP_KEY = "chip_key"
        private const val LABEL = "label"
        private const val LINE = "line"

        /** 받아 둔 카드와 겹쳐 받지 않은 카드 사유의 머리말. */
        private const val KEPT_PREFIX = "이미 받은 카드와 겹쳐 받지 않았다: "

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
