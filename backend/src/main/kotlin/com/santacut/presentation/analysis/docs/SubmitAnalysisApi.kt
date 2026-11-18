package com.santacut.presentation.analysis.docs

import com.santacut.presentation.analysis.request.SubmitAnalysisRequest
import com.santacut.presentation.analysis.response.AnalysisSubmissionResponse
import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "submitAnalysis",
    summary = "분석 제출",
    description = "분석 작업 하나를 대기열에 넣고 바로 응답합니다. 정규화한 이벤트 20~2,000개를 보내며, " +
        "이벤트 본문은 각각 1~2,000자, 전체 합계는 200,000자 이하입니다. 이벤트 시각은 올해 1월 1일(한국 시각) 이후여야 하고 " +
        "5분 뒤보다 늦으면 안 됩니다. 참가자는 최근 24시간 동안 최대 3번 제출할 수 있습니다. " +
        "원본 내보내기 파일은 POST /v1/analyses/files로 올립니다. 같은 Idempotency-Key로 같은 본문을 다시 보내면 처음 작업을 돌려주고, " +
        "다른 본문이면 충돌합니다. AI는 배려·공감(30%), 긍정·응원(25%), 존중하는 소통(25%), 협력·나눔(20%)을 평가하며, " +
        "서버가 근거 번호를 검증하고 가중 합계를 계산합니다.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [
        Parameter(
            name = "Idempotency-Key",
            `in` = ParameterIn.HEADER,
            required = true,
            description = "영문, 숫자, 밑줄, 하이픈으로 된 16~100자. 같은 요청 본문에만 다시 씁니다.",
        ),
        Parameter(name = ApiDocs.CSRF_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.CSRF_HEADER_DESCRIPTION),
    ],
    requestBody = RequestBody(required = true, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = SubmitAnalysisRequest::class))]),
    responses = [
        ApiResponse(responseCode = "202", description = "작업 접수", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = AnalysisSubmissionResponse::class))]),
        ApiResponse(responseCode = "400", description = "본문 형식·필드 검증 실패나 Idempotency-Key 헤더 누락(INVALID_REQUEST), 잘못된 Idempotency-Key(INVALID_IDEMPOTENCY_KEY), 이벤트 20개 미만(INSUFFICIENT_EVENTS), 이벤트 2,000개 초과(TOO_MANY_EVENTS), 범위를 벗어난 이벤트 시각이나 전체 글자 수(INVALID_EVENT_RANGE)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "세션이 없거나, 잘못됐거나, 만료됨(NOT_AUTHENTICATED)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "413", description = "요청 본문이 2MB를 넘음(REQUEST_TOO_LARGE). 앞단 프록시가 막으면 code 없이 올 수 있습니다.", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "같은 키로 다른 본문을 보냄(IDEMPOTENCY_CONFLICT) 또는 시즌 마감(SEASON_FROZEN)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "429", description = "참가자 하루 제출 한도 초과(DAILY_USER_LIMIT) 또는 같은 IP의 제출 한도 초과(CLIENT_RATE_LIMITED, 기본 24시간 20회). 본문 retry_at과 Retry-After 헤더(초)로 다시 낼 수 있는 시점을 알립니다.", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "503", description = "점수 산정용 AI 모델이 설정되지 않음(AI_NOT_CONFIGURED)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class SubmitAnalysisApi
