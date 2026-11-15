package com.santacut.presentation.share.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getPublicSharePage",
    summary = "공개 공유 페이지 보기",
    description = "소셜 미리보기용 Open Graph 메타데이터가 있는 서버 렌더링 HTML 페이지를 돌려줍니다. " +
        "공유 링크를 만든 순간 고정한 결과 스냅숏을 보여 줍니다.",
    tags = ["Public sharing"],
    parameters = [Parameter(name = "token", `in` = ParameterIn.PATH, required = true, description = ApiDocs.SHARE_TOKEN_DESCRIPTION)],
    responses = [
        ApiResponse(responseCode = "200", description = "공유 미리보기 메타데이터(Open Graph)가 있는 웹 페이지(HTML)", content = [Content(mediaType = "text/html", schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "404", description = "공유 토큰이 잘못됐거나, 취소됐거나, 없음(SHARE_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class PublicSharePageApi
