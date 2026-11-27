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
    description = "로그인한 참가자의 최신 결과(평가)로 만든 「팩폭 산타」 리캡 카드 덱(9:16 카드 5~8장)을 돌려줍니다. " +
        "총점 65점 이상이면 착한 아이(skin GIFT), 아래면 나쁜 아이(COAL)이고 재심은 없습니다. " +
        "모든 카드에 상단 바 값(rank, top_percent)과 score_text가 있습니다. rank는 순위 계산이 끝나면 참가자 수와 상관없이 늘 있고, " +
        "재업로드 직후처럼 최신 결과가 아직 순위에 반영되지 않았을 때(ranking_pending true)만 null입니다. 그동안은 이 API를 다시 불러 순위를 기다립니다. " +
        "top_percent는 순위 계산 참가자가 100명보다 적으면 null이고, 그때 상단 바는 '전국 N등'만 씁니다. " +
        "cards는 순서가 고정입니다. 선물 덱: VERDICT, CUTLINE, REASON(최대 3), CUT(최대 2), CONCLUSION. 석탄 덱: VERDICT, CUTLINE, CUT(최대 3), KEEP(1), CONCLUSION. " +
        "근거 카드(REASON, CUT, KEEP)의 quote는 서버가 본인 말에서 이름을 가리고 20자에서 자른 말풍선이고, null이면 칩을 크게 찍는 통계 카드입니다. 상대 말은 싣지 않습니다. " +
        "AI 문장(verdict_line, label, line, title, final_line)은 분석 때 쓰고 서버가 검증해 저장한 것이며, 검증을 통과하지 못하면 구간별 대체 문장입니다. 문장 안에는 숫자가 없고 숫자는 칩과 점수 필드로만 나갑니다. " +
        "옛 작가 프롬프트로 만든 결과도 통계 카드와 구간별 대체 결론으로 바꿔 돌려줍니다. " +
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
