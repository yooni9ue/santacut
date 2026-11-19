package com.santacut.domain.dossier.model

/**
 * 장면에 넣을 줄 하나의 글과 공유 가능 여부. 번호(id)는 장면을 만들 때 붙인다([SceneLines.numbered]).
 */
data class LineDraft(val text: String, val shareable: Boolean)
