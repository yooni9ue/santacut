package com.santacut.presentation.dossier.response

import com.santacut.application.dossier.result.DossierLineResult
import io.swagger.v3.oas.annotations.media.Schema

data class DossierLineResponse(
    @field:Schema(description = "장면 안의 줄 ID(l1부터)", example = "l1")
    val id: String,
    @field:Schema(description = "줄 글. 숫자는 서버가 단위까지 포맷했습니다.")
    val text: String,
    @field:Schema(description = "공유 카드에 실을 수 있는 줄인지. false면 화면에만 보여 줍니다(삐 처리, 하위 판정, 느린 답장).")
    val shareable: Boolean,
) {
    companion object {
        fun from(result: DossierLineResult): DossierLineResponse = DossierLineResponse(result.id, result.text, result.shareable)
    }
}
