package com.santacut.presentation.ranking.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.ranking.response.RankingResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getRankings",
    summary = "순위표 페이지 조회",
    description = "배송 순서대로 참가자를 최대 50명 돌려줍니다. offset은 0부터 시작하며 0~10,000입니다. " +
        "score_hundredths는 100분의 1점 단위 정수입니다(예: 8250은 82.50점). eta와 computed_at은 ISO-8601 UTC 시각이며 " +
        "화면에는 Asia/Seoul로 표시합니다. 배송 시뮬레이션은 12월 25일 0시(Asia/Seoul)에 출발해 배송 순서대로 " +
        "각 자치구 대표 좌표를 방문하며, 시속 100km 이동, 정거장마다 3분, 하루 6시간 작업을 가정합니다. " +
        "실제 배송 약속이 아닌 게임 시뮬레이션입니다.",
    tags = ["Results"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    parameters = [
        Parameter(
            name = "offset",
            `in` = ParameterIn.QUERY,
            required = false,
            description = "0부터 시작하는 행 위치(기본값 0, 범위 0~10,000)",
            schema = Schema(type = "integer", defaultValue = "0", minimum = "0", maximum = "10000"),
        ),
    ],
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "요청한 페이지의 순위표 행",
            content = [Content(mediaType = ApiDocs.JSON, array = ArraySchema(schema = Schema(implementation = RankingResponse::class)))],
        ),
        ApiResponse(responseCode = "400", description = "조회 위치(offset)가 0~10,000 범위를 벗어남(INVALID_OFFSET)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetRankingsApi
