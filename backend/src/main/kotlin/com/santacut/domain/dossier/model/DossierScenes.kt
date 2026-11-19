package com.santacut.domain.dossier.model

/**
 * 보여 줄 순서대로 놓은 조서 장면 목록.
 */
data class DossierScenes(private val values: List<DossierScene>) {
    fun toList(): List<DossierScene> = values.toList()

    /** [id] 장면. 없으면 null이다. */
    fun find(id: String): DossierScene? = values.find { scene -> scene.id == id }

    fun ids(): List<String> = values.map(DossierScene::id)
}
