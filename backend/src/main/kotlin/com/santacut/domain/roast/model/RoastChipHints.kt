package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonValue

/** 작가 입력의 칩 후보 목록(chips). JSON에서는 배열 하나로 나간다. */
class RoastChipHints(private val values: List<RoastChipHint>) {
    @JsonValue
    fun toList(): List<RoastChipHint> = values.toList()
}
