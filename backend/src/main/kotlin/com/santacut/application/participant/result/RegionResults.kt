package com.santacut.application.participant.result

import com.santacut.domain.participant.model.Regions

/**
 * 참가 가능한 지역 목록. 지역 코드 순서를 유지한다.
 */
class RegionResults(private val values: List<RegionResult>) {
    /** 목록 순서대로 각 지역 결과를 바꾼 결과. */
    fun <T> map(transform: (RegionResult) -> T): List<T> = values.map(transform)

    companion object {
        fun from(regions: Regions): RegionResults = RegionResults(regions.map(RegionResult::from))
    }
}
