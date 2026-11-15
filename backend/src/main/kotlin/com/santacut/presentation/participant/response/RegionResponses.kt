package com.santacut.presentation.participant.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.participant.result.RegionResults

/**
 * 참가 가능 지역 목록 응답. JSON에서는 [RegionResponse] 배열 하나로 나간다.
 */
class RegionResponses(@field:JsonValue private val values: List<RegionResponse>) {
    companion object {
        fun from(results: RegionResults): RegionResponses = RegionResponses(results.map(RegionResponse::from))
    }
}
