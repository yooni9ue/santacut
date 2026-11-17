package com.santacut.presentation.prompt.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.prompt.request.CreatePromptRequest
import com.santacut.presentation.prompt.response.PromptVersionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "createPrompt",
    summary = "프롬프트 새 판 저장",
    description = "이 이름의 가장 높은 판 다음 번호로 새 판을 저장합니다. 처음 쓰는 이름이면 1판입니다. " +
        "저장한 판은 바꾸지 않습니다. AI 채점(score)은 다음 호출부터 새 판을 씁니다. " + ApiDocs.PROMPT_ADMIN_NOTE,
    tags = ["Admin"],
    parameters = [
        Parameter(name = "name", `in` = ParameterIn.PATH, required = true, description = ApiDocs.PROMPT_NAME_DESCRIPTION),
        Parameter(name = ApiDocs.ADMIN_TOKEN_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.ADMIN_TOKEN_HEADER_DESCRIPTION),
        Parameter(name = ApiDocs.CSRF_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.CSRF_HEADER_DESCRIPTION),
    ],
    requestBody = RequestBody(required = true, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = CreatePromptRequest::class))]),
    responses = [
        ApiResponse(responseCode = "201", description = "새 판 저장 완료. 본문은 돌려주지 않습니다.", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = PromptVersionResponse::class))]),
        ApiResponse(responseCode = "400", description = "이름 형식 오류, content가 없거나 1~20,000자를 벗어남, 잘못된 요청 본문(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.ADMIN_TOKEN_REJECTED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.ADMIN_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = ApiDocs.PROMPT_ADMIN_DISABLED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class CreatePromptApi
