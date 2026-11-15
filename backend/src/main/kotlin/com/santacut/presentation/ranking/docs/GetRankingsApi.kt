package com.santacut.presentation.ranking.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.ranking.response.RankingBoardResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getRankings",
    summary = "순위표 페이지 조회",
    description = "로그인 없이 볼 수 있는 통합 순위표입니다. 가장 최근 순위 계산 결과를 배송 순서대로 최대 50줄 돌려주고, " +
        "시즌 연도, 참가자 수, 계산 시각, 확정 상태(PREVIEW, FINAL)를 함께 줍니다. 다음 페이지는 next_cursor를 cursor로 보내 읽고, " +
        "next_cursor가 null이면 마지막 페이지입니다. 아직 순위가 없으면 participant_count는 0, computed_at은 null, rows는 빈 배열입니다. " +
        "score_hundredths는 100분의 1점 단위 정수입니다(예: 8250은 82.50점). eta와 computed_at은 ISO-8601 UTC 시각이며 " +
        "화면에는 Asia/Seoul로 표시합니다. 배송 시뮬레이션은 12월 25일 0시(Asia/Seoul)에 출발해 배송 순서대로 " +
        "각 지역 대표 좌표를 방문하며, 시속 100km 이동, 정거장마다 3분, 하루 6시간 작업을 가정합니다. " +
        "실제 배송 약속이 아닌 게임 시뮬레이션입니다.",
    tags = ["Results"],
    parameters = [
        Parameter(
            name = "cursor",
            `in` = ParameterIn.QUERY,
            required = false,
            description = "이전 응답의 next_cursor. 첫 페이지는 보내지 않습니다. 값은 불투명 문자열로 다룹니다.",
            schema = Schema(type = "string"),
        ),
    ],
    responses = [
        ApiResponse(responseCode = "200", description = "순위표 한 페이지와 메타 정보", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = RankingBoardResponse::class))]),
        ApiResponse(responseCode = "400", description = "이전 응답이 준 값이 아닌 cursor(INVALID_CURSOR)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetRankingsApi
