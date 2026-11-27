package com.santacut.domain.roast.model

/**
 * 근거 카드에 쓸 수 있는 칩 후보 하나. [key]는 작가가 고르는 chip_key이고 [chip]은 서버가 센 칩 값이다.
 *
 * @property direction 칩 방향. 통계 카드도 이 방향이 맞는 종류에만 쓴다([ChipDirection.allows]).
 * @property dimension 방향이 가리키는 항목 점수 key. 근거 카드 영역 태그에 쓴다. 없으면 null이다.
 * @property topic 칩이 말하는 버릇([RoastFact.topic]). 통계 카드가 깎인 말과 같은 버릇을 살아남은 말로 띄우지 않게 견준다.
 */
data class RoastChipFact(
    val key: String,
    val chip: RoastChip,
    val direction: ChipDirection = ChipDirection.EITHER,
    val dimension: String? = null,
    val topic: String = key,
) {
    /**
     * 통계 카드로 [kind] 자리에 쓸 수 있는지. 방향이 맞아야 하고([ChipDirection.allows]), 살아남은 말은 좋은 쪽 칩만 쓴다.
     * 어느 쪽으로도 읽히는 입버릇('했어?')은 석탄 덱에서 깎인 버릇이라 대체 살아남은 말로 띄우지 않는다.
     */
    fun fallsBackAs(kind: EvidenceKind): Boolean =
        if (kind == EvidenceKind.KEEP) direction == ChipDirection.GOOD else direction.allows(kind)
}
