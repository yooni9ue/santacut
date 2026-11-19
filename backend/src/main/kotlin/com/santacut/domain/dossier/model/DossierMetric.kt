package com.santacut.domain.dossier.model

import kotlin.math.roundToLong

/**
 * 엔진이 계산하는 극값 후보 지표. metrics.json의 key는 이 목록에 있어야 한다. 값을 글로 바꾸는 단위는 metrics.json의 unit이 정한다.
 *
 * @property family 같은 계열은 극값 장에 하나만 나온다
 * @property chart 극값 장에 붙일 그래프. 없으면 null이다.
 */
enum class DossierMetric(val key: String, val family: MetricFamily, val chart: ChartKind?) {
    LAUGH_DENSITY("laugh_density", MetricFamily.LAUGH, null),
    LAUGH_MAX_RUN("laugh_max_run", MetricFamily.LAUGH, null),
    CRY_DENSITY("cry_density", MetricFamily.CRY, null),
    EMOJI_DENSITY("emoji_density", MetricFamily.EMOJI, null),
    NIGHT_RATIO("night_ratio", MetricFamily.CLOCK, ChartKind.HOURLY),
    PEAK_HOUR("peak_hour", MetricFamily.CLOCK, ChartKind.HOURLY),
    MONTH_RATIO("month_ratio", MetricFamily.VOLUME, ChartKind.MONTHLY),
    MESSAGE_COUNT("message_count", MetricFamily.VOLUME, ChartKind.MONTHLY),
    ACTIVE_DAYS("active_days", MetricFamily.VOLUME, ChartKind.MONTHLY),
    AVG_LENGTH("avg_length", MetricFamily.LENGTH, null),
    SHORT_RATIO("short_ratio", MetricFamily.LENGTH, null),
    LONGEST_MESSAGE_LENGTH("longest_message_length", MetricFamily.LENGTH, null),
    BURST_AVG("burst_avg", MetricFamily.BURST, null),
    BURST_MAX("burst_max", MetricFamily.BURST, null),
    QUESTION_RATIO("question_ratio", MetricFamily.PUNCTUATION, null),
    EXCLAIM_RATIO("exclaim_ratio", MetricFamily.PUNCTUATION, null),
    REPLY_MEDIAN_MINUTES("reply_median_minutes", MetricFamily.REPLY, null),
    KIND_WORDS_DENSITY("kind_words_density", MetricFamily.KIND, null),
    BLEEP_DENSITY("bleep_density", MetricFamily.BLEEP, null),
    ;

    /** [stats]에서 이 지표의 값. 값이 없으면(1:1이 아닌 답장, ㅋ 없는 연타 등) null이다. */
    fun valueIn(stats: DossierStats): Double? = when (this) {
        LAUGH_DENSITY -> stats.laugh.laughDensity
        LAUGH_MAX_RUN -> stats.laugh.laughMaxRun?.length?.toDouble()
        CRY_DENSITY -> stats.laugh.cryDensity
        EMOJI_DENSITY -> stats.emoji.density
        NIGHT_RATIO -> stats.clock.nightRatio
        PEAK_HOUR -> stats.clock.peakHour?.toDouble()
        MONTH_RATIO -> stats.clock.monthRatio
        else -> valueOfCountsIn(stats)
    }

    /** 지표마다 덧붙는 슬롯. 연타는 그 달·시간대, 월별 기복은 가장 붐빈 달, 최다 시각은 그 시간대다. */
    fun extraSlots(stats: DossierStats): Slots = when (this) {
        LAUGH_MAX_RUN -> Slots.of(
            MONTH to stats.laugh.laughMaxRun?.month?.let(DossierFormat::month),
            HOUR to stats.laugh.laughMaxRun?.hour?.let(DossierFormat::hour),
        )
        MONTH_RATIO -> Slots.of(MONTH to stats.clock.busiestMonth?.let(DossierFormat::month))
        PEAK_HOUR -> Slots.of(HOUR to stats.clock.peakHour?.let(DossierFormat::hour))
        else -> Slots.EMPTY
    }

    /** 공유 카드에 실어도 되는지. 느린 답장(높은 쪽)과 삐 처리는 화면 전용이다. */
    fun isCardSafe(isHigh: Boolean): Boolean = this != BLEEP_DENSITY && !(this == REPLY_MEDIAN_MINUTES && isHigh)

    private fun valueOfCountsIn(stats: DossierStats): Double? = when (this) {
        MESSAGE_COUNT -> stats.volume.messageCount.toDouble()
        ACTIVE_DAYS -> stats.volume.activeDays.toDouble()
        AVG_LENGTH -> stats.style.avgLength
        SHORT_RATIO -> stats.style.shortRatio
        LONGEST_MESSAGE_LENGTH -> stats.style.longestMessageLength.toDouble()
        BURST_AVG -> stats.style.burstAvg
        BURST_MAX -> stats.style.burstMax.toDouble()
        QUESTION_RATIO -> stats.style.questionRatio
        EXCLAIM_RATIO -> stats.style.exclaimRatio
        REPLY_MEDIAN_MINUTES -> stats.reply.medianMinutes?.toDouble()
        KIND_WORDS_DENSITY -> densityOf(stats.words.kindWords.total(), stats.messageCount())
        else -> densityOf(stats.words.bleepCount, stats.messageCount())
    }

    private fun densityOf(count: Int, messageCount: Int): Double? =
        if (messageCount == 0) null else (count * PER_HUNDRED * TENTHS / messageCount).roundToLong() / TENTHS

    companion object {
        private const val MONTH = "month"
        private const val HOUR = "hour"
        private const val PER_HUNDRED = 100.0
        private const val TENTHS = 10.0

        fun ofKey(key: String): DossierMetric? = entries.find { metric -> metric.key == key }
    }
}
