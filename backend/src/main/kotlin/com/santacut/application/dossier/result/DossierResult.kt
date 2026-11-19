package com.santacut.application.dossier.result

import com.santacut.domain.dossier.model.DossierView

/**
 * 조립한 조서. [tone]은 normal이나 mild다.
 */
data class DossierResult(
    val title: String,
    val tone: String,
    val scenes: DossierSceneResults,
) {
    companion object {
        fun from(view: DossierView): DossierResult = DossierResult(view.title, view.tone.code(), DossierSceneResults.from(view.scenes))
    }
}
