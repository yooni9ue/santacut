package com.santacut.presentation.participant.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.participant.result.RegionResult
import io.swagger.v3.oas.annotations.media.Schema

data class RegionResponse(
    @field:Schema(description = "지역 코드. selectable이 true일 때만 등록에 씁니다.", example = "KR-11-650")
    val code: String,
    @field:Schema(description = "한국어 지역 이름", example = "영등포구")
    val name: String,
    @field:JsonProperty("parent_code")
    @field:Schema(description = "상위 지역(시·도) 코드. 최상위 지역이면 null입니다.", example = "KR-11", nullable = true)
    val parentCode: String?,
    @field:Schema(
        description = "참가 지역으로 고를 수 있는지. 하위 지역이 있는 시·도는 false이고 parent 쿼리로 하위 지역을 읽습니다. " +
            "구가 없는 시·도는 그 자체로 true입니다.",
        example = "true",
    )
    val selectable: Boolean,
) {
    companion object {
        fun from(result: RegionResult): RegionResponse =
            RegionResponse(result.code, result.name, result.parentCode, result.selectable)
    }
}
