package com.santacut.application.dossier.result

import com.santacut.domain.dossier.model.DossierScenes

/**
 * 보여 줄 순서대로 놓은 조서 장면 목록.
 */
data class DossierSceneResults(val values: List<DossierSceneResult>) {
    companion object {
        fun from(scenes: DossierScenes): DossierSceneResults = DossierSceneResults(scenes.toList().map(DossierSceneResult::from))
    }
}
