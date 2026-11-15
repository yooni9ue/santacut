package com.santacut.presentation.analysis.response

import com.santacut.application.analysis.result.AnalysisSubmissionResult
import io.swagger.v3.oas.annotations.media.Schema

data class AnalysisSubmissionResponse(
    @field:Schema(description = "분석 작업 UUID", format = "uuid", example = "9ef3fca1-ec60-4c43-a231-d378d246ee34")
    val id: String,
    @field:Schema(
        description = "처음 진행 단계. 같은 요청을 다시 보냈다면 기존 작업의 현재 단계입니다. 값은 상태 조회 응답과 같습니다.",
        example = "QUEUED",
        allowableValues = ["VALIDATING", "QUEUED", "ANALYZING", "RANKING_PENDING", "SUCCEEDED", "FAILED"],
    )
    val status: String,
) {
    companion object {
        fun from(result: AnalysisSubmissionResult): AnalysisSubmissionResponse =
            AnalysisSubmissionResponse(result.id, result.status)
    }
}
