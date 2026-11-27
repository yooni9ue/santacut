package com.santacut.domain.roast.model

/** 근거 카드에 쓸 수 있는 칩 후보 하나. [key]는 작가가 고르는 chip_key이고 [chip]은 서버가 센 칩 값이다. */
data class RoastChipFact(val key: String, val chip: RoastChip)
