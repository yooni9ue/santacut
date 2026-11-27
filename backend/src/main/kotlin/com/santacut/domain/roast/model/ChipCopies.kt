package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 칩별 대체 문구 목록(roast/fallback.json의 chips). JSON에서는 배열 하나로 읽고 쓴다. */
class ChipCopies @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<ChipCopy>,
) {
    @JsonValue
    fun toList(): List<ChipCopy> = values.toList()

    /** [chipKey] 칩의 문구. key가 같은 것을 먼저, 없으면 머리말이 맞는 것을 쓴다. 없으면 null이다. */
    fun find(chipKey: String): ChipCopy? =
        values.find { copy -> chipKey in copy.keys } ?: values.find { copy -> copy.covers(chipKey) }

    /** [chipKeys] 가운데 문구가 없는 칩 key. */
    fun missing(chipKeys: List<String>): List<String> = chipKeys.filter { key -> find(key) == null }

    /** 모든 칩 문구의 고정 라벨과 한 줄. */
    fun plainTexts(): List<String> = values.flatMap(ChipCopy::plainTexts)

    /** 빈 key·머리말이거나 고정 문장이 없는 문구가 있는지. */
    fun hasMalformed(): Boolean = values.any(ChipCopy::isMalformed)

    companion object {
        val EMPTY = ChipCopies(emptyList())
    }
}
