package com.santacut.domain.analysis.model

/**
 * 채점에 쓰는 본인 메시지 전체. 여기서 항목별 낱말 신호와 표본을 만든다.
 */
class ScoringMessages(private val values: List<ScoringMessage>) {
    fun size(): Int = values.size

    /** 고르는 규칙은 [SampleCandidates]가 정한다. */
    fun sample(): SampleMessages = SampleCandidates.from(values).pick()

    /** 항목마다 낱말이 든 메시지 수, 그런 메시지가 있는 주 수, [sample]에서 낱말이 든 줄 수. */
    fun signals(sample: SampleMessages): DimensionSignals {
        val activeWeeks = values.map { message -> message.weekStart() }.distinct().size
        return DimensionSignals(ScoreTheme.entries.map { theme -> signalOf(theme, activeWeeks, sample) })
    }

    private fun signalOf(theme: ScoreTheme, activeWeeks: Int, sample: SampleMessages): DimensionSignal {
        val hits = values.filter { message -> message.mentions(theme) }
        val hitWeeks = hits.map { message -> message.weekStart() }.distinct().size
        return DimensionSignal(theme.dimension, hits.size, hitWeeks, activeWeeks, sample.countMentioning(theme))
    }
}
