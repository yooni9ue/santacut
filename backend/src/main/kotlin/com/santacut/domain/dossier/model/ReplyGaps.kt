package com.santacut.domain.dossier.model

/**
 * 상대 메시지 뒤 오래 조용했다가 온 본인 첫 메시지까지의 간격(분) 목록.
 */
class ReplyGaps(private val values: List<Long>) {
    /** 간격의 중앙값(분). 표본이 [MIN_SAMPLES]개보다 적으면 null이다. 짝수 개면 가운데 두 값의 평균(내림)이다. */
    fun medianMinutes(): Int? {
        if (values.size < MIN_SAMPLES) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        val median = if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
        return median.toInt()
    }

    companion object {
        const val MIN_SAMPLES = 10
    }
}
