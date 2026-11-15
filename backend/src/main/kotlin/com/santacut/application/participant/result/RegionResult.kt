package com.santacut.application.participant.result

import com.santacut.domain.participant.model.Region

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
