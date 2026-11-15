package com.santacut.presentation.response

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
    @field:Schema(description = "기계가 읽는 오류 코드. 프레임워크가 만든 오류에는 없습니다.", example = "SEASON_FROZEN")
    val code: String? = null,
    @field:Schema(description = "사람이 읽는 한국어 오류 메시지. 프레임워크가 만든 오류에는 없습니다.", example = "시즌이 마감되어 더 이상 분석을 받을 수 없습니다.")
    val message: String? = null,
)
