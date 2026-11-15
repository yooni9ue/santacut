package com.santacut.application.participant.result

import com.santacut.domain.participant.model.Region

/**
 * 지역 하나와 참가 지역으로 고를 수 있는지 여부.
 */
data class RegionResult(
    val code: String,
    val name: String,
    val parentCode: String?,
    val selectable: Boolean,
) {
    companion object {
        fun from(region: Region, selectable: Boolean): RegionResult =
            RegionResult(region.code, region.name, region.parentCode, selectable)
    }
}
