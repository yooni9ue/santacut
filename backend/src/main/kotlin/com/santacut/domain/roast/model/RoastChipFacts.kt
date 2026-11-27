package com.santacut.domain.roast.model

/**
 * 근거 카드 칩 후보 목록. 앞자리일수록 세게 튀는 칩이다. 작가가 쓰지 못했거나 검증에 떨어졌을 때 통계 카드(말풍선 없이 칩만 크게)를 만든다.
 */
class RoastChipFacts(private val values: List<RoastChipFact>) {
    fun size(): Int = values.size

    /**
     * 슬롯 수 [slots]만큼 덱 순서(이유, 깎인 말, 살아남은 말)로 통계 카드를 만든다. 칩은 덱 안에서 한 번만 쓰고,
     * 그 종류에 방향이 맞는 칩([ChipDirection.allows])만 앞자리(더 튀는 칩)부터 쓴다. 중립 칩(달별 기복, 근무시간 비율 등)은 쓰지 않는다.
     * 고마워 칩은 다른 사실 뒤에 있어 나쁜 쪽 칩이 모자랄 때만 깎인 말 한 장을 채운다. 맞는 칩이 모자라면 있는 만큼만 만든다
     * (빈 종류는 덱에 실을 때 항목 점수 카드로 채운다, [RoastEvidences.completedFor]).
     * 이미 덱에 있는 근거 [taken]의 칩은 쓰지 않고, 대체 문구는 [taken]의 같은 종류 카드 다음 순번부터 돌려 쓴다.
     */
    fun fallbackEvidence(slots: RoastSlots, fallback: RoastFallback, taken: RoastEvidences = RoastEvidences.EMPTY): RoastEvidences {
        val used = taken.chipKeys().toMutableSet()
        val cards = EvidenceKind.entries.flatMap { kind ->
            (0 until slots.countOf(kind)).mapNotNull { index ->
                values.firstOrNull { fact -> fact.key !in used && fact.direction.allows(kind) }?.let { fact ->
                    used += fact.key
                    fallback.evidenceOf(kind, taken.countOf(kind) + index, fact)
                }
            }
        }
        return RoastEvidences(cards)
    }

    companion object {
        val EMPTY = RoastChipFacts(emptyList())
    }
}
