package com.santacut.presentation.dossier.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.dossier.result.DossierSceneResults

/**
 * 조서 장면 목록. JSON에서는 [DossierSceneResponse] 배열 하나로 나간다.
 */
class DossierSceneResponses(@field:JsonValue private val values: List<DossierSceneResponse>) {
    companion object {
        fun from(results: DossierSceneResults): DossierSceneResponses =
            DossierSceneResponses(results.values.map(DossierSceneResponse::from))
    }
}
