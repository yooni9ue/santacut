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
    description = "로그인한 참가자의 최신 결과를 돌려줍니다. 점수와 순위는 늘 같은 심사 것을 묶습니다. " +
        "새 심사가 아직 순위에 반영되지 않았으면 이전 스냅숏의 점수와 순위를 주고 ranking_pending을 true로 둡니다. " +
        "첫 순위 계산 전이면 점수와 판정만 주고 rank, delivery_sequence, eta, computed_at은 null입니다. " +
        "stage는 PREVIEW(시즌 마감 전)나 FINAL(마감 후)이고, tied가 true면 공동 순위입니다. " +
        "score_hundredths는 100으로 나눠 표시하는 정수이고, eta와 computed_at은 ISO-8601 UTC 시각입니다.",
    tags = ["Results"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "최신 점수, 순위, 시뮬레이션 배송 시각", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = MyResultResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "아직 점수가 나온 심사가 없음(RESULT_NOT_FOUND)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetMyResultApi
