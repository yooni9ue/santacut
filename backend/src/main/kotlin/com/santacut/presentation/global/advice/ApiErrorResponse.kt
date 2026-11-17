package com.santacut.presentation.global.advice

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ApiErrorResponse(
    @field:Schema(description = "오류 발생 시각(UTC)", format = "date-time", example = "2026-11-15T12:00:00Z")
    val timestamp: String,
    @field:Schema(description = "응답 상태 코드(HTTP)", example = "400")
    val status: Int,
    @field:Schema(description = "응답 상태 문구(HTTP)", example = "Bad Request")
    val error: String,
    @field:Schema(description = "요청 경로", example = "/v1/analyses")
    val path: String,
    @field:Schema(description = "기계가 읽는 오류 코드. CSRF 오류(403)나 없는 경로처럼 프레임워크가 만든 오류에는 없습니다.", example = "SEASON_FROZEN")
    val code: String? = null,
    @field:Schema(description = "사람이 읽는 한국어 오류 메시지. CSRF 오류(403)나 없는 경로처럼 프레임워크가 만든 오류에는 없습니다.", example = "시즌이 마감되어 더 이상 분석을 받을 수 없습니다.")
    val message: String? = null,
    @field:JsonProperty("retry_at")
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    @field:Schema(
        description = "다시 시도할 수 있는 시각(UTC). 요청 한도 초과(DAILY_USER_LIMIT, CLIENT_RATE_LIMITED, REGION_CHANGE_LIMITED), 업로드 혼잡(UPLOAD_BUSY), 진행 상황 스트림 상한(ANALYSIS_STREAM_LIMIT)에만 있고 Retry-After 헤더(초)와 같은 시점입니다.",
        format = "date-time",
        example = "2026-11-16T12:00:00Z",
        nullable = true,
    )
    val retryAt: String? = null,
)
