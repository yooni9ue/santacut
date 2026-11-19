package com.santacut.domain.dossier.model

/**
 * 장면의 줄 하나. [shareable]이 거짓인 줄(삐 처리, 하위 판정 등)은 공유 카드에 싣지 않는다.
 */
data class SceneLine(val id: String, val text: String, val shareable: Boolean)
