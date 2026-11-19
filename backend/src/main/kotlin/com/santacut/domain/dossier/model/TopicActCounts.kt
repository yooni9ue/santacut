package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 주제 key별 막([DossierAct] 순서)마다의 횟수. JSON에서는 주제 key를 이름으로, 막별 횟수 배열을 값으로 하는 객체 하나다.
 */
class TopicActCounts @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: Map<String, List<Int>>,
) {
    @JsonValue
    fun toMap(): Map<String, List<Int>> = values.toMap()

    /** [key] 주제가 [act] 막에 나온 횟수. 없으면 0이다. */
    fun countOf(key: String, act: DossierAct): Int = values[key]?.getOrNull(act.ordinal) ?: 0
}
