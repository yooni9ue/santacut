package com.santacut.domain.analysis.model

/**
 * AI에 보내는 본인 메시지 표본. 시각순이고, AI는 1부터 붙인 줄 번호를 근거 번호로 돌려준다.
 */
class ScoringSample(private val values: List<ScoringMessage>) {
    fun size(): Int = values.size

    /** 표본 본문 글자 수의 합. */
    fun totalLength(): Int = values.sumOf { it.length() }

    fun countMentioning(theme: ScoreTheme): Int = values.count { it.mentions(theme) }

    fun toList(): List<ScoringMessage> = values.toList()

    /** 시각순으로 1부터 시작하는 번호를 붙여 각 메시지를 바꾼다. */
    fun <T> mapNumbered(transform: (Int, ScoringMessage) -> T): List<T> =
        values.mapIndexed { index, message -> transform(index + FIRST_NUMBER, message) }

    companion object {
        private const val FIRST_NUMBER = 1
    }
}
