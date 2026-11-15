package com.santacut.presentation.docs

import com.santacut.presentation.response.ApiErrorResponse
import com.santacut.presentation.response.CsrfResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getCsrfToken",
    summary = "CSRF 토큰 발급",
    description = "쓰기 요청(POST, DELETE) 전에 호출합니다. 응답은 XSRF-TOKEN 쿠키도 설정하며, " +
        "토큰 값은 headerName이 가리키는 헤더에 넣어 보냅니다.",
    tags = ["Session"],
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "보낼 CSRF 헤더 이름과 토큰",
            headers = [Header(name = "Set-Cookie", description = "이후 쓰기 요청에 필요한 XSRF-TOKEN 쿠키", schema = Schema(type = "string"))],
            content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = CsrfResponse::class))],
        ),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetCsrfApi
