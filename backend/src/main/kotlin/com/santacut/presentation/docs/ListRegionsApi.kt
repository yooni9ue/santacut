package com.santacut.presentation.docs

import com.santacut.presentation.response.ApiErrorResponse
import com.santacut.presentation.response.RegionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "listRegions",
    summary = "참가 가능 지역 목록",
    description = "지금 참가 등록에 쓸 수 있는 서울 자치구 목록을 돌려줍니다.",
    tags = ["Identity"],
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "지역 코드와 표시 이름",
            content = [Content(mediaType = ApiDocs.JSON, array = ArraySchema(schema = Schema(implementation = RegionResponse::class)))],
        ),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class ListRegionsApi
