package com.santacut.application.dossier.result

import com.santacut.domain.dossier.model.SceneLines

/**
 * 장면의 줄 목록.
 */
data class DossierLineResults(val values: List<DossierLineResult>) {
    companion object {
        fun from(lines: SceneLines): DossierLineResults = DossierLineResults(lines.toList().map(DossierLineResult::from))
    }
}
