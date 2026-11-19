package com.santacut.domain.dossier.model

/**
 * 장면의 줄 목록. 줄 번호는 장면 안에서 l1부터 붙는다.
 */
data class SceneLines(private val values: List<SceneLine>) {
    fun toList(): List<SceneLine> = values.toList()

    fun isEmpty(): Boolean = values.isEmpty()

    companion object {
        val EMPTY = SceneLines(emptyList())
        private const val ID_PREFIX = "l"

        /** 줄 초안에 순서대로 번호를 붙인다. */
        fun numbered(drafts: List<LineDraft>): SceneLines =
            SceneLines(drafts.mapIndexed { index, draft -> SceneLine(ID_PREFIX + (index + 1), draft.text, draft.shareable) })
    }
}
