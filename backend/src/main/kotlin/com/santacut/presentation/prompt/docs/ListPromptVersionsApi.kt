package com.santacut.presentation.prompt.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.prompt.response.PromptVersionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "listPromptVersions",
    summary = "프롬프트 판 목록 조회",
    description = "이 이름의 모든 판을 높은 판부터 돌려줍니다. 본문은 빼고 이름, 판 번호, 저장 시각만 담습니다. " + ApiDocs.PROMPT_ADMIN_NOTE,
    tags = ["Admin"],
    parameters = [Parameter(name = "name", `in` = ParameterIn.PATH, required = true, description = ApiDocs.PROMPT_NAME_DESCRIPTION)],
    responses = [
        ApiResponse(responseCode = "200", description = "판 목록(높은 판부터)", content = [Content(mediaType = ApiDocs.JSON, array = ArraySchema(schema = Schema(implementation = PromptVersionResponse::class)))]),
        ApiResponse(responseCode = "400", description = "이름 형식 오류(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "이 이름의 판이 없음(PROMPT_NOT_FOUND). 또는 " + ApiDocs.PROMPT_ADMIN_DISABLED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class ListPromptVersionsApi
