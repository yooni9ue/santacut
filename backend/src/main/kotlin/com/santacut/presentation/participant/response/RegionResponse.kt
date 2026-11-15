package com.santacut.presentation.participant.response

import com.santacut.application.participant.result.RegionResult
import io.swagger.v3.oas.annotations.media.Schema

data class RegionResponse(
    @field:Schema(description = "등록할 때 쓰는 지역 코드", example = "KR-11-650")
    val code: String,
    @field:Schema(description = "한국어 지역 이름", example = "영등포구")
    val name: String,
) {
    companion object {
        fun from(result: RegionResult): RegionResponse = RegionResponse(result.code, result.name)
    }
}
