package com.santacut.presentation.participant.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.participant.request.RegisterParticipantRequest
import com.santacut.presentation.participant.response.ParticipantResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "registerParticipant",
    summary = "참가자 등록",
    description = "참가자와 로그인 세션을 만듭니다. 닉네임은 NFKC로 정규화하고 대소문자를 구분하지 않고 비교합니다. " +
        "먼저 CSRF 토큰을 받아야 합니다. 세션 값은 HttpOnly 쿠키로만 내려가며 JSON 본문에는 없습니다.",
    tags = ["Identity"],
    parameters = [
        Parameter(
            name = ApiDocs.CSRF_HEADER,
            `in` = ParameterIn.HEADER,
            required = true,
            description = "토큰 발급 API(GET /v1/csrf)가 돌려준 토큰. 그 응답의 XSRF-TOKEN 쿠키도 유지합니다.",
        ),
    ],
    requestBody = RequestBody(required = true, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = RegisterParticipantRequest::class))]),
    responses = [
        ApiResponse(
            responseCode = "201",
            description = "참가자 생성 완료. 세션 값은 HttpOnly 쿠키로 설정됩니다.",
            headers = [Header(name = "Set-Cookie", description = "세션 쿠키. 로컬은 sc_session, 운영은 __Host-sc_session입니다.", schema = Schema(type = "string"))],
            content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ParticipantResponse::class))],
        ),
        ApiResponse(responseCode = "400", description = "닉네임 형식 오류(INVALID_NICKNAME), 없거나 하위 지역을 골라야 하는 지역(UNSUPPORTED_REGION), 잘못된 요청 본문(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임(NICKNAME_TAKEN)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "413", description = "요청 본문이 2MB를 넘음(REQUEST_TOO_LARGE)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class RegisterParticipantApi
