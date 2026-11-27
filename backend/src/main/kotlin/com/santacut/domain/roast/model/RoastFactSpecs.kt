package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 지표를 작가 사실로 옮기는 규칙 목록(roast/facts.json의 facts). JSON에서는 배열 하나로 읽는다. */
class RoastFactSpecs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<RoastFactSpec>,
) {
    @JsonValue
    fun toList(): List<RoastFactSpec> = values.toList()

    fun violations(): List<String> =
        values.flatMap(RoastFactSpec::violations) +
            values.groupBy(RoastFactSpec::key).filterValues { same -> same.size > 1 }.keys.map { key -> "facts: key $key 중복" }

    /** 모든 지표 key. */
    fun keys(): List<String> = values.map(RoastFactSpec::key)

    /** [key] 지표의 규칙. 없으면(놀릴 거리로 쓰지 않는 지표) null이다. */
    fun find(key: String): RoastFactSpec? = values.find { spec -> spec.key == key }
}
