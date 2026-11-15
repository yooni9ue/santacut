package com.santacut.application.result

import com.santacut.domain.model.Region

/**
 * 참가 가능한 지역.
 */
data class RegionResult(
    val code: String,
    val name: String,
) {
    companion object {
        fun from(region: Region): RegionResult = RegionResult(region.code, region.name)
    }
}
