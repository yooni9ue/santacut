package com.santacut.presentation.ranking.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.ranking.response.MyResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getMyResult",
    summary = "내 최신 결과 조회",
    description = "로그인한 참가자의 최신 평가와 가장 최근에 발표한 순위 스냅숏을 돌려줍니다. " +
        "score_hundredths는 100으로 나눠 표시하는 정수이고, eta와 computed_at은 ISO-8601 UTC 시각입니다.",
    tags = ["Results"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "최신 점수, 순위, 시뮬레이션 배송 시각", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = MyResultResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "아직 순위가 계산된 결과가 없음(RESULT_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetMyResultApi
