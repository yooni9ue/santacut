package com.santacut.domain.dossier.model

/**
 * 조서 장면 하나. 프론트는 세로 스냅 스크롤로 한 장씩 보여 주고, [shareable]인 장만 저장(공유 카드) 버튼을 단다.
 *
 * @property speaker 화자 key(elf, rudolph, lawyer, santa)
 * @property bigValue 크게 보여 줄 값(입버릇, 숫자, 칭호). 없으면 null이다.
 * @property chart 막대 그래프. 없으면 null이다.
 */
data class DossierScene(
    val id: String,
    val kind: SceneKind,
    val speaker: String,
    val headline: String?,
    val bigValue: String?,
    val lines: SceneLines,
    val memo: String?,
    val chart: SceneChart?,
    val shareable: Boolean,
) {
    companion object {
        const val ELF = "elf"
        const val RUDOLPH = "rudolph"
        const val SANTA = "santa"
        const val EXTREME_ID_PREFIX = "extreme_"
    }
}
