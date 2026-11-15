package com.santacut.presentation.participant.docs

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
    operationId = "deleteMyData",
    summary = "내 데이터 삭제",
    description = "로그인한 참가자를 지웁니다. 참가 정보, 로그인 세션, 분석 작업, 평가, 순위 기록, 공유 링크가 함께 지워지며 되돌릴 수 없습니다. " +
        "시즌 마감 전이면 남은 참가자로 순위를 다시 계산합니다. 마감 뒤에는 다시 계산하지 않습니다. " +
        "이미 저장되거나 전달된 공유 이미지는 회수할 수 없습니다. 응답은 세션 쿠키를 만료시킵니다. 먼저 CSRF 토큰을 받아야 합니다.",
    tags = ["Identity"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [
        Parameter(name = ApiDocs.CSRF_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.CSRF_HEADER_DESCRIPTION),
    ],
    responses = [
        ApiResponse(
            responseCode = "204",
            description = "삭제 완료",
            headers = [Header(name = "Set-Cookie", description = "Max-Age=0으로 만료시킨 세션 쿠키", schema = Schema(type = "string"))],
        ),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "세션의 참가자가 더 이상 없음(PARTICIPANT_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class DeleteMyDataApi
