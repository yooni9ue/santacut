package com.santacut.domain.participant.model

/**
 * 지역 목록. 코드 순서를 유지한다.
 */
class Regions(private val values: List<Region>) {
    fun find(code: String): Region? = values.firstOrNull { it.code == code }

    /** [parentCode]의 바로 아래 지역. null이면 최상위 지역(시·도)이다. */
    fun childrenOf(parentCode: String?): Regions = Regions(values.filter { it.isChildOf(parentCode) })

    /** 하위 지역이 없는 지역만 참가 지역으로 고를 수 있다. 구가 없는 시·도는 그 자체로 고른다. */
    fun isSelectable(region: Region): Boolean = values.none { it.isChildOf(region.code) }

    /** 목록에 있고 고를 수 있는 지역 코드인지. */
    fun isSelectable(code: String): Boolean = find(code)?.let(::isSelectable) == true

    /** 코드 접두어가 맞는 지역 가운데 코드가 가장 긴, 즉 가장 가까운 상위 지역. 없으면 최상위 지역이다. */
    fun parentOf(region: Region): Region? = values.filter { it.isParentOf(region) }.maxByOrNull { it.code.length }

    /**
     * 상위 지역이 비어 있는 지역을 [parentOf]에 잇고, 새로 이어진 지역만 돌려준다.
     * 이미 이어진 지역은 건드리지 않으므로 여러 번 실행해도 결과가 같다.
     */
    fun linkParents(): Regions =
        Regions(values.filterNot(Region::isLinked).mapNotNull { child -> parentOf(child)?.let(child::linkTo) })

    fun toList(): List<Region> = values.toList()

    /** 목록 순서대로 각 지역을 바꾼 결과. */
    fun <T> map(transform: (Region) -> T): List<T> = values.map(transform)
}
