package com.santacut.presentation.analysis.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.analysis.result.AnalysisStatusResult
import io.swagger.v3.oas.annotations.media.Schema

data class AnalysisStatusResponse(
    @field:Schema(description = "분석 작업 UUID", format = "uuid")
    val id: String,
    @field:Schema(
        description = "현재 진행 단계. VALIDATING은 올린 원본 파일을 확인하는 중(원본 파일 제출만), " +
            "RANKING_PENDING은 심사가 끝나고 순위를 계산하는 중, FAILED는 실패나 근거 부족(error_code로 구분)입니다.",
        example = "QUEUED",
        allowableValues = ["VALIDATING", "QUEUED", "ANALYZING", "RANKING_PENDING", "SUCCEEDED", "FAILED"],
    )
    val status: String,
    @field:JsonProperty("error_code")
    @field:Schema(
        description = "기계가 읽는 실패 코드. 실패하기 전에는 null입니다.",
        allowableValues = [
            "SEASON_FROZEN", "EXPIRED", "RETRY_EXHAUSTED", "INSUFFICIENT_EVIDENCE", "ANALYSIS_FAILED",
            "UNSUPPORTED_FILE_FORMAT", "SPEAKER_NOT_IDENTIFIED", "INSUFFICIENT_EVENTS",
        ],
    )
    val errorCode: String?,
    @field:JsonProperty("assessment_id")
    @field:Schema(description = "분석이 성공하면 만들어지는 평가 UUID")
    val assessmentId: String?,
    @field:JsonProperty("created_at")
    @field:Schema(description = "작업 생성 시각(UTC)", format = "date-time")
    val createdAt: String,
    @field:JsonProperty("finished_at")
    @field:Schema(description = "작업이 끝난 시각(UTC). 검증, 대기, 실행 중에는 null입니다.", format = "date-time")
    val finishedAt: String?,
) {
    companion object {
        fun from(result: AnalysisStatusResult): AnalysisStatusResponse = AnalysisStatusResponse(
            result.id, result.status, result.errorCode, result.assessmentId,
            result.createdAt.toString(), result.finishedAt?.toString(),
        )
    }
}
