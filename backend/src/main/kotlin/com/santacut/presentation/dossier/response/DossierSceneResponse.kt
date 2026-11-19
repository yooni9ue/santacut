package com.santacut.presentation.dossier.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.dossier.result.DossierSceneResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class DossierSceneResponse(
    @field:Schema(description = "장면 ID. 극값 장은 extreme_1~extreme_3입니다.", example = "extreme_1")
    val id: String,
    @field:Schema(
        description = "장면 종류",
        allowableValues = ["teaser", "hook", "ledger", "extreme", "acts", "dossier", "verdict", "summons"],
    )
    val kind: String,
    @field:Schema(
        description = "화자 key. elf는 계수 요정, rudolph는 루돌프, lawyer는 변호 요정, santa는 산타입니다.",
        allowableValues = ["elf", "rudolph", "lawyer", "santa"],
    )
    val speaker: String,
    @field:Schema(description = "장면 제목. 없으면 null입니다.", nullable = true)
    val headline: String?,
    @field:JsonProperty("big_value")
    @field:Schema(description = "크게 보여 줄 값(입버릇, 서버가 포맷한 숫자, 칭호, 순위). 없으면 null입니다.", nullable = true, example = "100개당 212개")
    val bigValue: String?,
    @field:ArraySchema(
        arraySchema = Schema(description = "장면의 줄. 없으면 빈 배열입니다."),
        schema = Schema(implementation = DossierLineResponse::class),
    )
    val lines: DossierLineResponses,
    @field:Schema(description = "화자 메모 한 줄. 없으면 null입니다.", nullable = true)
    val memo: String?,
    @field:Schema(description = "막대 그래프. 없으면 null입니다.", nullable = true)
    val chart: DossierChartResponse?,
    @field:Schema(description = "이 장면을 공유 카드로 저장할 수 있는지. 티저는 false입니다.")
    val shareable: Boolean,
) {
    companion object {
        fun from(result: DossierSceneResult): DossierSceneResponse = DossierSceneResponse(
            result.id, result.kind, result.speaker, result.headline, result.bigValue, DossierLineResponses.from(result.lines),
            result.memo, result.chart?.let(DossierChartResponse::from), result.shareable,
        )
    }
}
