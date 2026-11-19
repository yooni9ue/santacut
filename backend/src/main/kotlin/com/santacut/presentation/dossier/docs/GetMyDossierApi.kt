package com.santacut.presentation.dossier.docs

import com.santacut.presentation.dossier.response.DossierResponse
import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getMyDossier",
    summary = "내 산타 조서 조회",
    description = "로그인한 참가자의 최신 결과(평가)로 만든 「2026 산타 조서」 장면 목록을 돌려줍니다. " +
        "재업로드 직후 순위 다시 계산 전(GET /v1/me/result의 ranking_pending이 true)에도 새 평가의 조서를 주며, 그동안 판결 장의 순위는 비어 있습니다. " +
        "서버는 분석 때 본인 메시지에서 센 집계값만 저장하고(원문 없음), 조회할 때마다 같은 문구로 장면을 조립합니다. " +
        "scenes는 보여 줄 순서대로 teaser, hook, ledger, extreme(최대 3), acts, dossier, verdict, summons이며, " +
        "본인 메시지가 100개 미만이면 ledger·acts·dossier가 빠지고 극값은 2장까지입니다. " +
        "shareable이 false인 장면과 줄(삐 처리, 하위 판정, 느린 답장)은 공유 카드에 싣지 않습니다. " +
        "tone이 mild면 죄목과 놀림 문구 없이 담백하게 나갑니다. tone 값은 화면 문구에만 쓰고 분석 이벤트나 로그에 싣지 않습니다. " +
        "결과가 아직 없으면 RESULT_NOT_FOUND, 이 기능 전에 받은 결과(또는 예전 집계 규칙으로 센 조서)라 조서가 없으면 DOSSIER_NOT_FOUND입니다. " +
        "DOSSIER_NOT_FOUND이면 시즌 마감 전에는 재업로드 안내를, 마감 뒤(재업로드 불가)에는 조서가 없다는 안내만 띄웁니다.",
    tags = ["Results"],
    security = [SecurityRequirement(name = ApiDocs.SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "칭호, 톤, 장면 목록", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = DossierResponse::class))]),
        ApiResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(
            responseCode = "404",
            description = "아직 점수가 나온 심사가 없음(RESULT_NOT_FOUND), 또는 최신 결과에 조서가 없음(DOSSIER_NOT_FOUND)",
            content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))],
        ),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class GetMyDossierApi
