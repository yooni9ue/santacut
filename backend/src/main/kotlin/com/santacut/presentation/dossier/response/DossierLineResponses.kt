package com.santacut.presentation.dossier.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.dossier.result.DossierLineResults

/**
 * 장면의 줄 목록. JSON에서는 [DossierLineResponse] 배열 하나로 나간다.
 */
class DossierLineResponses(@field:JsonValue private val values: List<DossierLineResponse>) {
    companion object {
        fun from(results: DossierLineResults): DossierLineResponses =
            DossierLineResponses(results.values.map(DossierLineResponse::from))
    }
}
