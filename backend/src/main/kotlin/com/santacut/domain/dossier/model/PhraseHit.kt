package com.santacut.domain.dossier.model

/**
 * 허용 어휘 하나를 본인 메시지에서 센 결과. 저장 전 TOP 목록을 고를 때만 쓴다.
 */
class PhraseHit(private val entry: PhraseEntry, val count: Int) {
    val key: String = entry.key

    fun specificity(messageCount: Int): Double = entry.specificity(count, messageCount)

    fun toTally(): PhraseTally = entry.tally(count)
}
