package com.santacut.presentation.share.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.share.response.CreateShareResponse
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
    operationId = "createShare",
    summary = "내 최신 결과 공유 링크 만들기",
    description = "현재 결과의 스냅숏을 담은, 취소할 수 있는 공개 링크를 만들거나 기존 링크를 다시 씁니다. " +
        "JSON 응답에는 HTML 페이지 주소와 1200×630 PNG 이미지 주소가 들어 있습니다. 먼저 CSRF 토큰을 받아야 합니다. " +
        "본문은 생략할 수 있고, include_quote=true면 본인 한마디를 카드에 싣습니다(기본 false). 같은 결과로 이미 만든 링크가 있으면 그 링크를 그대로 돌려주고 " +
        "include_quote는 바뀌지 않습니다. 카드에는 유형, 점수, 판정, 순위, 상위 %, 도착 예정일, 항목 막대 4개, 배지 1개, 가장 약한 항목이 들어가며 상대 말은 들어가지 않습니다.",
    tags = ["Sharing"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [Parameter(name = ApiDocs.CSRF_HEADER, `in` = ParameterIn.HEADER, required = true, description = ApiDocs.CSRF_HEADER_DESCRIPTION)],
    responses = [
        ApiResponse(responseCode = "200", description = "공유 링크를 만들었거나 다시 씀", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = CreateShareResponse::class))]),
        ApiResponse(responseCode = "400", description = "읽을 수 없는 JSON 본문이나 boolean이 아닌 include_quote(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = ApiDocs.FORBIDDEN_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "세션의 참가자가 더 이상 없음(PARTICIPANT_NOT_FOUND) 또는 공유할 결과의 평가를 찾지 못함(RESULT_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "공유할 순위 결과가 아직 없음(NO_RANKED_RESULT)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class CreateShareApi
