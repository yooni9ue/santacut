package com.santacut.presentation.participant.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.participant.request.ChangeRegionRequest
import com.santacut.presentation.participant.response.CurrentParticipantResponse
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
    operationId = "changeRegion",
    summary = "내 지역 변경",
    description = "로그인한 참가자의 지역을 바꾸고 같은 트랜잭션에서 순위와 도착 예정 시각을 다시 계산합니다. " +
        "시즌이 마감(12월 25일 0시, Asia/Seoul)된 뒤에는 바꿀 수 없습니다. 이미 만든 공유 링크는 만든 당시 결과를 그대로 보여 줍니다. " +
        "먼저 CSRF 토큰을 받아야 합니다.",
    tags = ["Identity"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [
        Parameter(name = ApiDocs.CSRF_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.CSRF_HEADER_DESCRIPTION),
    ],
    requestBody = RequestBody(required = true, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ChangeRegionRequest::class))]),
    responses = [
        ApiResponse(responseCode = "200", description = "바뀐 참가자 정보", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = CurrentParticipantResponse::class))]),
        ApiResponse(responseCode = "400", description = "없거나 하위 지역을 골라야 하는 지역(UNSUPPORTED_REGION), 잘못된 요청 본문(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "세션의 참가자가 더 이상 없음(PARTICIPANT_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "시즌 마감 뒤 변경(SEASON_FROZEN)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class ChangeRegionApi
