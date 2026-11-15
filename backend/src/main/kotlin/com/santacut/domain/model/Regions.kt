package com.santacut.domain.model

/**
 * 지역 목록. 코드 순서를 유지한다.
 */
class Regions(private val values: List<Region>) {
    fun find(code: String): Region? = values.firstOrNull { it.code == code }

    /** 목록 순서대로 각 지역을 바꾼 결과. */
    fun <T> map(transform: (Region) -> T): List<T> = values.map(transform)
}
