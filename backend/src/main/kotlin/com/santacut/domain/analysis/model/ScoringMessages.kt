package com.santacut.domain.analysis.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 채점에 쓰는 본인 메시지 전체. 여기서 활동 통계, 항목별 낱말 신호, 표본을 만든다. AI에는 신호와 표본만 보낸다.
 */
class ScoringMessages(private val values: List<ScoringMessage>) {
    /** 서버 쪽에서만 쓰는 활동 통계. AI에는 보내지 않는다. */
    fun toFeatures(): ScoringFeatures {
        val activeDays = ActiveDays.of(values.map { it.date() })
        return ScoringFeatures(
            eventCount = values.size,
            activeDays = activeDays.size(),
            messagesPerActiveDay = ratio(values.size, activeDays.size(), PER_DAY_SCALE),
            hourCounts = HourCounts(values.groupingBy { it.hourBucket() }.eachCount()),
            longestStreakDays = activeDays.longestStreak(),
            laughEmojiRatio = ratio(values.count { it.hasLaughOrEmoji() }, values.size, RATIO_SCALE),
        )
    }

    /** 고르는 규칙은 [SampleCandidates]가 정한다. */
    fun sample(): ScoringSample = SampleCandidates.from(values).pick()

    /** 항목마다 낱말이 든 메시지 수, 그런 메시지가 있는 주 수, [sample]에서 낱말이 든 줄 수. */
    fun signals(sample: ScoringSample): DimensionSignals {
        val activeWeeks = values.map { it.weekStart() }.distinct().size
        return DimensionSignals(ScoreTheme.entries.map { theme -> signalOf(theme, activeWeeks, sample) })
    }

    private fun signalOf(theme: ScoreTheme, activeWeeks: Int, sample: ScoringSample): DimensionSignal {
        val hits = values.filter { it.mentions(theme) }
        val hitWeeks = hits.map { it.weekStart() }.distinct().size
        return DimensionSignal(theme.dimension, hits.size, hitWeeks, activeWeeks, sample.countMentioning(theme))
    }

    private fun ratio(numerator: Int, denominator: Int, scale: Int): BigDecimal =
        if (denominator == 0) {
            BigDecimal.ZERO.setScale(scale)
        } else {
            BigDecimal(numerator).divide(BigDecimal(denominator), scale, RoundingMode.HALF_UP)
        }

    companion object {
        private const val PER_DAY_SCALE = 1
        private const val RATIO_SCALE = 2
    }
}
