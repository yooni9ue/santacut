package com.santacut.application.dossier.result

import com.santacut.domain.dossier.model.DossierScene

/**
 * 조서 장면 하나. [kind]는 teaser, hook, ledger, extreme, acts, dossier, verdict, summons 중 하나다.
 */
data class DossierSceneResult(
    val id: String,
    val kind: String,
    val speaker: String,
    val headline: String?,
    val bigValue: String?,
    val lines: DossierLineResults,
    val memo: String?,
    val chart: DossierChartResult?,
    val shareable: Boolean,
) {
    companion object {
        fun from(scene: DossierScene): DossierSceneResult = DossierSceneResult(
            scene.id, scene.kind.code(), scene.speaker, scene.headline, scene.bigValue, DossierLineResults.from(scene.lines),
            scene.memo, scene.chart?.let(DossierChartResult::from), scene.shareable,
        )
    }
}
