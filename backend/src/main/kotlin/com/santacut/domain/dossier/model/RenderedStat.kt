package com.santacut.domain.dossier.model

/**
 * 통계 key 하나로 채운 문구(죄목, 공로, 주석). [shareable]이 거짓이면(느린 답장 등) 카드에 싣지 않는다.
 */
data class RenderedStat(val key: String, val text: String, val shareable: Boolean) {
    /** 방향 접미사(_low)를 뗀 통계 key. 죄목과 공로가 같은 통계를 쓰는지 가릴 때 쓴다. */
    fun statKey(): String = key.removeSuffix(MetricReading.LOW_SUFFIX)
}
