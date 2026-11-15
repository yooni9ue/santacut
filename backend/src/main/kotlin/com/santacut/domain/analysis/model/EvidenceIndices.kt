package com.santacut.domain.analysis.model

/**
 * AI 모델이 항목마다 돌려준 근거 이벤트 번호 목록. 번호는 1부터 시작하고, 해석할 수 없는 값은 -1이다.
 */
class EvidenceIndices(private val values: List<List<Int>>) {
    /** 모든 항목에 근거 번호가 하나 이상 있는지. */
    fun isComplete(): Boolean = values.size == ScoreDimension.entries.size && values.all { it.isNotEmpty() }

    /** 1~[eventCount] 범위를 벗어난 첫 근거 번호. 모두 범위 안이면 null이다. */
    fun firstOutOfRange(eventCount: Int): Int? = values.flatten().firstOrNull { it !in FIRST_INDEX..eventCount }

    companion object {
        private const val FIRST_INDEX = 1
    }
}
