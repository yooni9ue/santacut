package com.santacut.presentation.analysis.docs

import com.santacut.presentation.analysis.request.UploadAnalysisRequest
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
    operationId = "uploadAnalysisFile",
    summary = "원본 파일로 분석 제출",
    description = "카카오톡 대화 TXT나 인스타그램 message_1.json을 그대로 올려 분석 작업을 만들고 바로 응답합니다. " +
        "작업은 VALIDATING으로 시작합니다. 서버가 파일 형식을 확인하고 본인(self_speaker) 메시지만 이벤트로 바꾼 뒤 QUEUED로 넘깁니다. " +
        "다른 참여자의 메시지는 버리고 AI에 보내지 않습니다. 최근 90일 안의 최신 이벤트 2,000개, 200,000자까지만 씁니다. " +
        "검증 실패는 상태 조회의 error_code(UNSUPPORTED_FILE_FORMAT, SPEAKER_NOT_IDENTIFIED, INSUFFICIENT_EVENTS)로 알립니다. " +
        "원본과 본인 이름은 검증이 끝나면 지우고, 늦어도 24시간 뒤에는 지웁니다. 파일 이름은 저장하지 않습니다. " +
        "하루 제출 한도와 Idempotency-Key 규칙은 POST /v1/analyses와 같고 두 경로가 한도를 함께 셉니다. " +
        "같은 키로 같은 파일·출처·본인 이름을 다시 보내면 처음 작업을 돌려주고, 하나라도 다르면 충돌합니다.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [
        Parameter(
            name = "Idempotency-Key",
            `in` = ParameterIn.HEADER,
            required = true,
            description = "영문, 숫자, 밑줄, 하이픈으로 된 16~100자. 같은 요청에만 다시 씁니다.",
        ),
        Parameter(name = ApiDocs.CSRF_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.CSRF_HEADER_DESCRIPTION),
    ],
    requestBody = RequestBody(required = true, content = [Content(mediaType = ApiDocs.MULTIPART, schema = Schema(implementation = UploadAnalysisRequest::class))]),
    responses = [
        ApiResponse(responseCode = "202", description = "작업 접수. status는 VALIDATING이고, 같은 요청을 다시 보냈다면 기존 작업의 현재 단계입니다.", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = AnalysisSubmissionResponse::class))]),
        ApiResponse(responseCode = "400", description = "파트 누락·필드 검증 실패나 Idempotency-Key 헤더 누락(INVALID_REQUEST), 잘못된 Idempotency-Key(INVALID_IDEMPOTENCY_KEY)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "같은 키로 다른 요청을 보냄(IDEMPOTENCY_CONFLICT) 또는 시즌 마감(SEASON_FROZEN)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "413", description = "파일이 20MB를 넘음(FILE_TOO_LARGE). 앞단 프록시가 막으면 code 없이 올 수 있으며 같은 뜻으로 다룹니다.", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "422", description = "빈 파일, zip 파일, 바이너리 파일(UNSUPPORTED_FILE_FORMAT)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "429", description = "참가자 하루 제출 한도 초과(DAILY_USER_LIMIT). 본문 retry_at과 Retry-After 헤더(초)로 다시 낼 수 있는 시점을 알립니다.", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "503", description = "점수 산정용 AI 모델이 설정되지 않음(AI_NOT_CONFIGURED)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class UploadAnalysisApi
