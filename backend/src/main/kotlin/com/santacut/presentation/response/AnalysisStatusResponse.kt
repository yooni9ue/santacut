package com.santacut.presentation.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.result.AnalysisStatusResult
import io.swagger.v3.oas.annotations.media.Schema

data class AnalysisStatusResponse(
    @field:Schema(description = "분석 작업 UUID", format = "uuid")
    val id: String,
    @field:Schema(
        description = "현재 작업 상태",
        allowableValues = ["QUEUED", "RUNNING", "SUCCEEDED", "FAILED", "INSUFFICIENT_DATA"],
    )
    val status: String,
    @field:JsonProperty("error_code")
    @field:Schema(description = "기계가 읽는 실패 코드. 실패하기 전에는 null입니다.")
    val errorCode: String?,
    @field:JsonProperty("assessment_id")
    @field:Schema(description = "분석이 성공하면 만들어지는 평가 UUID")
    val assessmentId: String?,
    @field:JsonProperty("created_at")
    @field:Schema(description = "작업 생성 시각(UTC)", format = "date-time")
    val createdAt: String,
    @field:JsonProperty("finished_at")
    @field:Schema(description = "작업이 끝난 시각(UTC). 대기나 실행 중에는 null입니다.", format = "date-time")
    val finishedAt: String?,
) {
    companion object {
        fun from(result: AnalysisStatusResult): AnalysisStatusResponse = AnalysisStatusResponse(
            result.id, result.status, result.errorCode, result.assessmentId,
            result.createdAt.toString(), result.finishedAt?.toString(),
        )
    }
}
