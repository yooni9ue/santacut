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
    operationId = "getPublicShareImage",
    summary = "공개 공유 이미지 받기",
    description = "소셜 공유용 1200×630 PNG 카드를 그려 돌려줍니다. 실시간 순위가 아니라 저장된 공유 스냅숏을 그립니다.",
    tags = ["Public sharing"],
    parameters = [Parameter(name = "token", `in` = ParameterIn.PATH, required = true, description = ApiDocs.SHARE_TOKEN_DESCRIPTION)],
    responses = [
        ApiResponse(responseCode = "200", description = "결과 카드 이미지(PNG)", content = [Content(mediaType = "image/png", schema = Schema(type = "string", format = "binary"))]),
        ApiResponse(responseCode = "404", description = "공유 토큰이 잘못됐거나, 취소됐거나, 없음(SHARE_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class PublicShareImageApi
