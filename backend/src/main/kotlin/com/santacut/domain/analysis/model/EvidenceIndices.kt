package com.santacut.domain.analysis.model

/**
 * AI 모델이 항목마다 돌려준 근거 번호 목록. 번호는 1부터 시작하고, 해석할 수 없는 값은 -1이다.
 */
class EvidenceIndices(private val values: List<List<Int>>) {
    /** 모든 항목에 근거 번호가 하나 이상 있는지. */
    fun isComplete(): Boolean = values.size == ScoreDimension.entries.size && values.all { it.isNotEmpty() }

    /** 항목마다 1~[maxIndex] 범위 밖 번호와 겹친 번호를 지운 목록. 다시 묻지 않고 남은 번호로 판단하기 위해서다. */
    fun cleaned(maxIndex: Int): EvidenceIndices =
        EvidenceIndices(values.map { indices -> indices.filter { it in FIRST_INDEX..maxIndex }.distinct() })

    /** [cleaned]가 지우는 번호 수. */
    fun discardedCount(maxIndex: Int): Int = totalCount() - cleaned(maxIndex).totalCount()

    private fun totalCount(): Int = values.sumOf { it.size }

    companion object {
        private const val FIRST_INDEX = 1
    }
}
