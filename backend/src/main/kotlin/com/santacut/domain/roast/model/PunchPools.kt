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

    /** 모든 풀의 대체 문장. */
    fun fallbacks(): List<String> = values.values.flatMap(PunchTemplates::fallbacks)

    /** 모든 풀. */
    fun pools(): List<PunchTemplates> = values.values.toList()

    /**
     * 풀마다 표지가 [minStructures]개 이상인지, 같은 표지 틀이 둘 이상 없는지와 틀마다의 위반. [field]는 위반 설명 머리말(punch.bands 등)이다.
     * 같은 표지 틀을 한 풀에 두 번 두지 않아 '올해 너는 ○○이야~' 같은 한 틀 가족이 풀마다 하나로 묶인다.
     */
    fun templateViolations(field: String, minStructures: Int, rules: RoastRules): List<String> = values.flatMap { (name, pool) ->
        listOfNotNull(
            "$field.$name: 서로 다른 펀치 표지가 ${minStructures}개 이상이어야 한다".takeIf { pool.markerCount() < minStructures },
            "$field.$name: 같은 표지 틀이 둘 이상 있다".takeIf { pool.markerCount() < pool.size() },
        ) + pool.violations("$field.$name", rules)
    }

    companion object {
        val EMPTY = PunchPools(emptyMap())
    }
}
