package com.santacut.presentation.docs

import com.santacut.presentation.response.AnalysisResultResponse
import com.santacut.presentation.response.AnalysisStatusResponse
import com.santacut.presentation.response.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "streamAnalysisEvents",
    summary = "분석 진행 상황과 결과 스트림",
    description = "서버 전송 이벤트(Server-Sent Events) 스트림입니다. 1초마다 최대 150초 동안 상태를 확인합니다. " +
        "이벤트 이름은 analysis(작업 상태), result(최종 점수·순위·도착 예정 시각), error(실패로 끝남)입니다. " +
        "작업이 끝나지 않았을 때만 다시 연결합니다. 작업을 제출한 참가자만 구독할 수 있습니다.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [Parameter(name = "id", `in` = ParameterIn.PATH, required = true, description = "분석 제출 API(POST /v1/analyses)가 돌려준 UUID")],
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "이벤트 스트림(text/event-stream). analysis와 error 이벤트는 AnalysisStatusResponse, result 이벤트는 AnalysisResultResponse를 씁니다.",
            content = [
                Content(
                    mediaType = "text/event-stream",
                    schema = Schema(oneOf = [AnalysisStatusResponse::class, AnalysisResultResponse::class], format = "event-stream"),
                ),
            ],
        ),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "작업이 없거나 다른 참가자의 작업(NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class StreamAnalysisApi
