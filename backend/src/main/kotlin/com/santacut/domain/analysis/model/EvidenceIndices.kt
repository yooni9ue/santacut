package com.santacut.domain.analysis.model

/**
 * AI 모델이 항목마다 돌려준 근거 줄 번호 목록. 번호는 1부터 시작하고, 해석할 수 없는 값은 -1이다.
 */
class EvidenceIndices(private val values: List<List<Int>>) {
    /** 모든 항목의 근거 목록이 들어왔는지. 근거가 약한 항목은 빈 목록일 수 있다. */
    fun isComplete(): Boolean = values.size == ScoreDimension.entries.size

    /** [position]번째 항목의 근거 번호. 없으면 빈 목록이다. */
    fun at(position: Int): List<Int> = values.getOrNull(position).orEmpty()

    /** 항목마다 [lines] 범위 밖 번호와 겹친 번호를 지운 목록. 다시 묻지 않고 남은 번호로 판단하기 위해서다. */
    fun cleaned(lines: IntRange): EvidenceIndices =
        EvidenceIndices(values.map { indices -> indices.filter { it in lines }.distinct() })

    /** [cleaned]가 지우는 번호 수. */
    fun discardedCount(lines: IntRange): Int = totalCount() - cleaned(lines).totalCount()

    private fun totalCount(): Int = values.sumOf { it.size }
}
