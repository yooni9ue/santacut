package com.santacut.presentation.analysis.docs

import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
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
    operationId = "getAnalysis",
    summary = "분석 작업 상태 조회",
    description = "분석 작업 하나의 현재 진행 단계를 돌려줍니다. 화면은 status가 SUCCEEDED나 FAILED가 될 때까지 " +
        "몇 초 간격으로 다시 조회합니다. SUCCEEDED가 되면 GET /v1/me/result로 점수와 순위를 읽습니다. " +
        "작업을 제출한 참가자만 볼 수 있습니다.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [Parameter(name = "id", `in` = ParameterIn.PATH, required = true, description = "분석 제출 API(POST /v1/analyses)가 돌려준 UUID")],
    responses = [
        ApiResponse(responseCode = "200", description = "현재 진행 단계", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = AnalysisStatusResponse::class))]),
        ApiResponse(responseCode = "400", description = "id가 UUID 형식이 아님(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "작업이 없거나 다른 참가자의 작업(NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetAnalysisApi
