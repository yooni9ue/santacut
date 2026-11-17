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
    description = "로그인한 참가자의 지역을 바꾸고 순위와 도착 예정 시각 다시 계산을 요청합니다. 다시 계산은 30초쯤 안에 모아서 하며, " +
        "그동안 내 결과의 도착 예정 시각은 이전 지역 기준입니다. 같은 지역을 보내면 아무것도 바꾸지 않고 횟수도 세지 않습니다. " +
        "지역은 24시간에 2번, 한 시즌에 5번까지 바꿀 수 있습니다(서버 설정). " +
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
        ApiResponse(responseCode = "429", description = "24시간 한도를 넘음(REGION_CHANGE_LIMITED, retry_at·Retry-After 이후 다시 시도), 시즌 한도를 넘음(REGION_CHANGE_SEASON_LIMIT, retry_at 없음)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class ChangeRegionApi
