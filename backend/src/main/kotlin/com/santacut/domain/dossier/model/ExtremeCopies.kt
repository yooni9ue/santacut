package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 지표 key별 극값 문구(copy.json의 `extremes`).
 */
class ExtremeCopies @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: Map<String, ExtremeCopy>,
) {
    @JsonValue
    fun toMap(): Map<String, ExtremeCopy> = values.toMap()

    fun of(key: String): ExtremeCopy? = values[key]

    fun has(key: String): Boolean = key in values

    /** 검사 위치를 붙인 모든 템플릿 묶음. */
    fun groups(): Map<String, Templates> = values.flatMap { (key, copy) -> copy.groups("$PATH.$key").toList() }.toMap()

    /** [speakers]에 없는 화자를 쓴 지표. */
    fun speakerViolations(speakers: Map<String, String>): List<String> =
        values.filterValues { copy -> copy.speaker !in speakers }.keys.map { key -> "$PATH.$key: speakers에 없는 화자다" }

    companion object {
        private const val PATH = "extremes"
    }
}
