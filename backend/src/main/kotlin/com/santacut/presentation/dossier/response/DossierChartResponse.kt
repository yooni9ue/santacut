package com.santacut.presentation.dossier.response

import com.santacut.application.dossier.result.DossierChartResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class DossierChartResponse(
    @field:Schema(description = "그래프 종류. bar24는 0~23시 24칸, bar12는 1~12월 12칸입니다.", allowableValues = ["bar24", "bar12"])
    val type: String,
    @field:ArraySchema(arraySchema = Schema(description = "칸마다의 본인 메시지 수"))
    val values: List<Int>,
) {
    companion object {
        fun from(result: DossierChartResult): DossierChartResponse = DossierChartResponse(result.type, result.values)
    }
}
