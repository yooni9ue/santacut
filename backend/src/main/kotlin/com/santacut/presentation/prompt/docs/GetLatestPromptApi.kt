package com.santacut.presentation.prompt.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.prompt.response.PromptResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getLatestPrompt",
    summary = "프롬프트 최신 판 조회",
    description = "이 이름의 가장 높은 판을 본문과 함께 돌려줍니다. AI 호출이 지금 쓰는 판입니다. " + ApiDocs.PROMPT_ADMIN_NOTE,
    tags = ["Admin"],
    parameters = [
        Parameter(name = "name", `in` = ParameterIn.PATH, required = true, description = ApiDocs.PROMPT_NAME_DESCRIPTION),
        Parameter(name = ApiDocs.ADMIN_TOKEN_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.ADMIN_TOKEN_HEADER_DESCRIPTION),
    ],
    responses = [
        ApiResponse(responseCode = "200", description = "최신 판", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = PromptResponse::class))]),
        ApiResponse(responseCode = "400", description = "이름 형식 오류(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.ADMIN_TOKEN_REJECTED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.ADMIN_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "이 이름의 판이 없음(PROMPT_NOT_FOUND). 또는 " + ApiDocs.PROMPT_ADMIN_DISABLED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetLatestPromptApi
