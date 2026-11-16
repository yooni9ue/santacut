package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.TranscriptTurn
import com.santacut.domain.analysis.model.TranscriptTurns
import java.time.Duration

/**
 * AI 모델에 보내는 대화문 줄 목록. 턴마다 `번호|화자|본문` 한 줄이고, 앞 턴과 [MIN_GAP] 이상 벌어지면
 * 그 앞에 `(3시간 뒤)`, `(2일 뒤)` 같은 번호 없는 간격 줄을 넣는다. 시각 자체는 보내지 않는다.
 */
class ScoreModelLines(private val values: List<String>) {
    fun toText(): String = values.joinToString(LINE_SEPARATOR)

    companion object {
        private const val LINE_SEPARATOR = "\n"
        private const val SEPARATOR = "|"
        private val MIN_GAP: Duration = TranscriptTurn.MERGE_GAP
        private const val HOURS_PER_DAY = 24L

        /** [firstNumber]부터 줄 번호를 붙인다. */
        fun from(turns: TranscriptTurns, firstNumber: Int): ScoreModelLines =
            ScoreModelLines(turns.mapNumbered(firstNumber, ::linesOf).flatten())

        private fun linesOf(number: Int, turn: TranscriptTurn, previous: TranscriptTurn?): List<String> =
            listOfNotNull(previous?.let { gapLine(turn.gapAfter(it)) }, "$number$SEPARATOR${turn.speaker}$SEPARATOR${turn.text}")

        private fun gapLine(gap: Duration): String? = when {
            gap < MIN_GAP -> null
            gap.toHours() < HOURS_PER_DAY -> "(${gap.toHours()}시간 뒤)"
            else -> "(${gap.toDays()}일 뒤)"
        }
    }
}
