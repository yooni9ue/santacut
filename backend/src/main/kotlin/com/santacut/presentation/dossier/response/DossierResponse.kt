package com.santacut.presentation.dossier.response

import com.santacut.application.dossier.result.DossierResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class DossierResponse(
    @field:Schema(description = "서버가 극값 지표로 조립한 칭호", example = "새벽반 웃음 공장장")
    val title: String,
    @field:Schema(
        description = "문구 톤. normal은 기본, mild는 죄목과 놀림 문구를 끈 담백 톤입니다. 화면 문구에만 쓰고 분석 이벤트나 로그에 싣지 않습니다.",
        allowableValues = ["normal", "mild"],
    )
    val tone: String,
    @field:ArraySchema(
        arraySchema = Schema(description = "보여 줄 순서대로 놓은 장면 목록. 세로 스냅 스크롤로 한 장씩 보여 줍니다."),
        schema = Schema(implementation = DossierSceneResponse::class),
    )
    val scenes: DossierSceneResponses,
) {
    companion object {
        fun from(result: DossierResult): DossierResponse =
            DossierResponse(result.title, result.tone, DossierSceneResponses.from(result.scenes))
    }
}
