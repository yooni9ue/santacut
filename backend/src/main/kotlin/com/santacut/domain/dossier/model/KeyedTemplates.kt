package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.analysis.model.ScoreDimension

/**
 * key별 문구 템플릿(copy.json의 `charges`, `memo_by_category` 같은 객체). 파일에 적은 key 순서를 지킨다.
 */
class KeyedTemplates @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: Map<String, Templates>,
) {
    @JsonValue
    fun toMap(): Map<String, Templates> = values.toMap()

    fun of(key: String): Templates? = values[key]

    fun keys(): List<String> = values.keys.toList()

    /** 검사 위치를 알리도록 key 앞에 [path]를 붙인 템플릿 묶음. */
    fun prefixed(path: String): Map<String, Templates> = values.mapKeys { (key, _) -> "$path.$key" }

    /** [required] key 중 없는 것. */
    fun missing(required: List<String>, path: String): List<String> =
        required.filterNot(values::containsKey).map { key -> "$path: $key 문구가 없다" }

    /** 채점 항목 key 중 없는 것. */
    fun missingDimensions(path: String): List<String> = missing(ScoreDimension.entries.map(ScoreDimension::key), path)
}
