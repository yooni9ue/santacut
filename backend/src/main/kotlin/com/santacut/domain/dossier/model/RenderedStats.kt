package com.santacut.domain.dossier.model

/**
 * 통계 key로 채운 문구 목록. 죄목과 공로가 같은 통계를 쓰지 않게 쓴 통계 key를 알려 준다.
 */
class RenderedStats(private val values: List<RenderedStat>) {
    /** 쓴 통계 key(방향 접미사를 뗀 것). */
    fun statKeys(): Set<String> = values.map(RenderedStat::statKey).toSet()

    fun drafts(): List<LineDraft> = values.map { stat -> LineDraft(stat.text, stat.shareable) }

    fun firstDraft(): LineDraft? = values.getOrNull(0)?.let { stat -> LineDraft(stat.text, stat.shareable) }
}
