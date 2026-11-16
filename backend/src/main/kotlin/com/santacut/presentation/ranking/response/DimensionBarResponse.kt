package com.santacut.presentation.ranking.response

import com.santacut.application.ranking.result.DimensionBarResult
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class DimensionBarResponse(
    @field:Schema(
        description = "채점 항목 키",
        allowableValues = ["care_empathy", "positivity_support", "respectful_communication", "cooperation_sharing"],
    )
    val key: String,
    @field:Schema(description = "화면에 보여 줄 항목 이름", allowableValues = ["공감", "응원", "존중", "협력"])
    val label: String,
    @field:Schema(description = "항목 점수(0~100, 소수 둘째 자리까지)", example = "71.67")
    val score: BigDecimal,
) {
    companion object {
        fun from(result: DimensionBarResult): DimensionBarResponse = DimensionBarResponse(result.key, result.label, result.score)
    }
}
