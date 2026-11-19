package com.santacut.application.dossier.result

import com.santacut.domain.dossier.model.SceneLine

/**
 * 장면의 줄 하나. [shareable]이 거짓이면 공유 카드에 싣지 않는다.
 */
data class DossierLineResult(val id: String, val text: String, val shareable: Boolean) {
    companion object {
        fun from(line: SceneLine): DossierLineResult = DossierLineResult(line.id, line.text, line.shareable)
    }
}
