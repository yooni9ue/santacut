package com.santacut.presentation.analysis.docs

import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "streamAnalysisStatus",
    summary = "분석 작업 진행 상황 스트림(SSE)",
    description = "분석 작업 하나의 진행 단계를 text/event-stream으로 밀어 줍니다. 연결하자마자 현재 단계를 한 번 보내고, " +
        "단계가 바뀔 때마다 이벤트를 하나씩 보냅니다. 이벤트 이름은 status, data는 상태 조회(GET /v1/analyses/{id})와 " +
        "같은 JSON, id는 진행 단계 순번(VALIDATING 0, QUEUED 1, ANALYZING 2, RANKING_PENDING 3, SUCCEEDED 4, FAILED 5)입니다. " +
        "다시 대기열로 돌아가면 id가 작아질 수 있습니다. SUCCEEDED나 FAILED를 보낸 뒤 서버가 연결을 닫습니다. " +
        "15초마다 주석 줄(: heartbeat)을 보내고, 5분이 지나면 서버가 닫습니다. 브라우저 EventSource는 Last-Event-ID를 붙여 " +
        "다시 연결하고, 서버는 다시 연결할 때마다 현재 단계를 처음부터 보냅니다. 이미 끝난 작업이면 끝난 단계를 보내고 바로 닫습니다. " +
        "참가자 한 명은 스트림을 하나만 열 수 있으며 새로 열면 이전 스트림이 닫힙니다. 서버 전체 스트림 수가 상한에 닿으면 503 " +
        "ANALYSIS_STREAM_LIMIT과 Retry-After를 줍니다. 이때나 연결 오류가 나면 상태 조회 폴링(2~3초 간격)으로 바꿉니다. " +
        "작업을 제출한 참가자만 볼 수 있습니다.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [
        Parameter(name = "id", `in` = ParameterIn.PATH, required = true, description = "분석 제출 API가 돌려준 UUID"),
        Parameter(
            name = "Last-Event-ID", `in` = ParameterIn.HEADER, required = false,
            description = "다시 연결할 때 브라우저가 붙이는 마지막 이벤트 id. 서버는 값과 상관없이 현재 단계를 다시 보냅니다.",
        ),
    ],
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "SSE 스트림. 이벤트마다 `event: status`, `id: <단계 순번>`, `data: <상태 조회 JSON>` 세 줄입니다.",
            content = [Content(mediaType = ApiDocs.EVENT_STREAM, schema = Schema(implementation = AnalysisStatusResponse::class))],
        ),
        ApiResponse(responseCode = "400", description = "id가 UUID 형식이 아님(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "작업이 없거나 다른 참가자의 작업(NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(
            responseCode = "503",
            description = "서버 전체 스트림 수가 상한에 닿음(ANALYSIS_STREAM_LIMIT). retry_at·Retry-After 뒤에 다시 열거나 폴링으로 바꿉니다.",
            headers = [Header(name = "Retry-After", description = "다시 열어 볼 수 있을 때까지 기다릴 초", schema = Schema(type = "integer"))],
            content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))],
        ),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class StreamAnalysisStatusApi
