package com.santacut.domain.roast.model

/**
 * 근거 카드에 쓸 수 있는 칩 후보 하나. [key]는 작가가 고르는 chip_key이고 [chip]은 서버가 센 칩 값이다.
 *
 * @property direction 칩 방향. 통계 카드도 이 방향이 맞는 종류에만 쓴다([ChipDirection.allows]).
 * @property dimension 방향이 가리키는 항목 점수 key. 근거 카드 영역 태그에 쓴다. 없으면 null이다.
 */
data class RoastChipFact(
    val key: String,
    val chip: RoastChip,
    val direction: ChipDirection = ChipDirection.EITHER,
    val dimension: String? = null,
)
