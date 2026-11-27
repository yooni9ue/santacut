package com.santacut.domain.roast.model

/**
 * 근거 카드 칩 후보 목록. 앞자리일수록 세게 튀는 칩이다. 작가가 쓰지 못했거나 검증에 떨어졌을 때 통계 카드(말풍선 없이 칩만 크게)를 만든다.
 */
class RoastChipFacts(private val values: List<RoastChipFact>) {
    fun size(): Int = values.size

    /**
     * 슬롯 수 [slots]만큼 덱 순서(이유, 깎인 말, 살아남은 말)로 통계 카드를 만든다. 칩은 덱 안에서 한 번만 쓴다.
     * 점수 깎인 말은 [preferredCutKey] 칩(고마워 0번)을 먼저 쓴다. 칩이 모자라면 있는 만큼만 만든다.
     */
    fun fallbackEvidence(slots: RoastSlots, fallback: RoastFallback, preferredCutKey: String? = null): RoastEvidences {
        val used = mutableSetOf<String>()
        val cards = EvidenceKind.entries.flatMap { kind ->
            (0 until slots.countOf(kind)).mapNotNull { index ->
                pickFor(kind, used, preferredCutKey)?.let { fact ->
                    used += fact.key
                    fallback.evidenceOf(kind, index, fact)
                }
            }
        }
        return RoastEvidences(cards)
    }

    private fun pickFor(kind: EvidenceKind, used: Set<String>, preferredCutKey: String?): RoastChipFact? {
        val open = values.filterNot { fact -> fact.key in used }
        val preferred = open.find { fact -> kind == EvidenceKind.CUT && fact.key == preferredCutKey }
        return preferred ?: open.firstOrNull { fact -> fact.key != preferredCutKey || kind == EvidenceKind.CUT }
    }

    companion object {
        val EMPTY = RoastChipFacts(emptyList())
    }
}
