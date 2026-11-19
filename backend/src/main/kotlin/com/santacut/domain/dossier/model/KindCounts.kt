package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 따뜻한 말 key별 횟수. JSON에서는 말 key를 이름으로, 횟수를 값으로 하는 객체 하나다.
 */
class KindCounts @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: Map<String, Int>,
) {
    @JsonValue
    fun toMap(): Map<String, Int> = values.toMap()

    fun countOf(key: String): Int = values[key] ?: 0

    /** 모든 따뜻한 말의 횟수 합. */
    fun total(): Int = values.values.sum()
}
