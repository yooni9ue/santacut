package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.SampleMessages

/**
 * AI 모델에 보내는 표본 메시지 줄 목록. 표본의 시각순으로 1부터 번호를 붙이고, 주가 바뀔 때마다 머리줄을 하나 넣는다.
 */
class ScoreModelEvents(private val values: List<ScoreModelEvent>) {
    fun toText(): String = values.flatMapIndexed { position, event -> linesOf(position, event) }.joinToString(LINE_SEPARATOR)

    private fun linesOf(position: Int, event: ScoreModelEvent): List<String> {
        val startsWeek = position == 0 || !values[position - 1].isInSameWeekAs(event)
        return if (startsWeek) listOf(event.weekHeader(), event.toLine()) else listOf(event.toLine())
    }

    companion object {
        private const val LINE_SEPARATOR = "\n"

        fun from(sample: SampleMessages): ScoreModelEvents =
            ScoreModelEvents(sample.mapNumbered { number, message -> ScoreModelEvent(number, message.weekStart(), message.text) })
    }
}
