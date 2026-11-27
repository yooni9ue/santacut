package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.analysis.model.GiftGrade

/** 이름(등급 이름이나 판정)별 펀치 틀 풀(roast/punch.json의 bands, skins). JSON에서는 객체 하나로 읽고 쓴다. */
class PunchPools @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: Map<String, PunchTemplates>,
) {
    @JsonValue
    fun toMap(): Map<String, PunchTemplates> = values.toMap()

    /** [name] 풀. 없으면 빈 풀이다. */
    fun of(name: String): PunchTemplates = values[name] ?: PunchTemplates.EMPTY

    /** [names] 가운데 풀이 없는 이름. */
    fun missing(names: List<String>): List<String> = names.filterNot(values::containsKey)

    /** 풀이 없는 등급 이름([GiftGrade.name]). 구간 풀(bands)은 모든 등급에 있어야 한다. */
    fun missingGrades(): List<String> = missing(GiftGrade.entries.map(GiftGrade::name))

    /** 모든 풀. */
    fun pools(): List<PunchTemplates> = values.values.toList()

    /**
     * 풀마다의 위반. 표지가 [minStructures]개 이상이고 유행어 없는 대체 문장이 하나 이상 있어야 하며, 틀마다 표지를 품어야 한다.
     * [field]는 위반 설명 머리말(punch.bands 등)이다.
     */
    fun violations(field: String, minStructures: Int, rules: RoastRules): List<String> = values.flatMap { (name, pool) ->
        listOfNotNull(
            "$field.$name: 서로 다른 펀치 표지가 ${minStructures}개 이상이어야 한다".takeIf { pool.markerCount() < minStructures },
            "$field.$name: 유행어 없는 대체 문장이 하나 이상 있어야 한다".takeUnless { pool.hasPlain(rules) },
        ) + pool.violations("$field.$name", rules)
    }

    companion object {
        val EMPTY = PunchPools(emptyMap())
    }
}
