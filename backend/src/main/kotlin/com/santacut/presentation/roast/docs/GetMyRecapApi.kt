package com.santacut.presentation.roast.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.roast.response.RecapResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getMyRecap",
    summary = "내 팩폭 산타 리캡 카드 덱 조회",
    description = "로그인한 참가자의 최신 결과(평가)로 만든 「팩폭 산타」 리캡 카드 덱(9:16 카드 여러 장)을 돌려줍니다. " +
        "총점 65점 이상이면 선물(skin GIFT), 아래면 석탄(COAL)이고 재심은 없습니다. 모든 카드에 score_text가 있어 상단에 점수를 크게 찍습니다. " +
        "cards는 순서가 고정입니다: VERDICT(점수·컷 거리·도장·등급·순위·도착 시각·칭호·판결), ROAST 세 장(서버 숫자 칩·놀림·뒤집기), " +
        "LETTER(산타 쪽지), STORY(세 막 줄거리, 있을 때만), ARCHETYPE(산타 유형), DIMENSIONS(4항목 막대). " +
        "문장은 분석 때 AI 작가가 쓰고 서버가 검증해 저장한 것이며, 검증을 통과하지 못한 필드는 서버 대체 문장입니다. 문장 안에는 숫자가 없고 숫자는 칩과 점수 필드로만 나갑니다. " +
        "농담을 끈 화면은 verdict 대신 verdict_mild, stamp 대신 stamp_mild, share_text 대신 share_text_mild를 씁니다. " +
        "재업로드 직후 순위 다시 계산 전(ranking_pending true)이나 순위 계산 참가자가 표본 기준(100명)보다 적으면 rank는 null입니다. " +
        "결과가 아직 없으면 RESULT_NOT_FOUND, 이 기능 전에 받은 결과라 카드 문장이 없으면 RECAP_NOT_FOUND입니다.",
    tags = ["Results"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "스킨, 점수, 공유 문구, 카드 목록", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = RecapResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(
            responseCode = "404",
            description = "아직 점수가 나온 심사가 없음(RESULT_NOT_FOUND), 또는 최신 결과에 카드 문장이 없음(RECAP_NOT_FOUND)",
            content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))],
        ),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetMyRecapApi
