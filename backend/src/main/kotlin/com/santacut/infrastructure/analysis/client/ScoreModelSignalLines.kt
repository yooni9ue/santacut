package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.DimensionSignal
import com.santacut.domain.analysis.model.DimensionSignals

/**
 * AI 모델에 보내는 `[signals]` 블록. 채점 항목마다 `care_empathy hit=41 weeks=9/13 shown=12` 한 줄이다.
 * 줄의 뜻은 채점 프롬프트 2판이 설명한다.
 */
class ScoreModelSignalLines(private val values: List<String>) {
    fun toText(): String = values.joinToString(LINE_SEPARATOR)

    companion object {
        private const val LINE_SEPARATOR = "\n"

        fun from(signals: DimensionSignals): ScoreModelSignalLines = ScoreModelSignalLines(signals.map(::lineOf))

        private fun lineOf(signal: DimensionSignal): String =
            "${signal.dimension.key} hit=${signal.hits} weeks=${signal.hitWeeks}/${signal.activeWeeks} shown=${signal.shown}"
    }
}
