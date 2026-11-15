package com.santacut.presentation.analysis.docs

import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getLatestAnalysis",
    summary = "내 최근 분석 작업 상태 조회",
    description = "로그인한 참가자가 가장 최근에 제출한 분석 작업의 진행 단계를 돌려줍니다. " +
        "재방문 때 진행 중인 작업이면 심사 화면으로, 성공했으면 결과 화면으로 보내는 데 씁니다. " +
        "응답 형식은 GET /v1/analyses/{id}와 같습니다.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "가장 최근 작업의 진행 단계", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = AnalysisStatusResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "제출한 작업이 없음(NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetLatestAnalysisApi
