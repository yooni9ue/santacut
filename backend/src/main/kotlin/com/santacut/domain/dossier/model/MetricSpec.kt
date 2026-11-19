package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 극값 후보 지표 하나와 그 시드 기준 분포(metrics.json). 백분위는 p10·p50·p90 사이를 선형으로 잇고 0~100으로 자른다.
 *
 * @property unit 값을 글로 바꾸는 단위. per100(100개당 N개), ratio(0~1을 N%), count(N개), chars(N자), minutes(N분대), hour(밤 N시대),
 * days(N일), times(N배) 중 하나다.
 * @property minMessages 이 지표가 극값 후보가 되려면 필요한 본인 메시지 수
 * @property mockable 거짓이면 놀림 문구 대신 중립 문구를 쓴다(ㅠ 밀도 등)
 */
data class MetricSpec(
    val key: String,
    val label: String,
    val unit: String,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    @JsonProperty("min_messages") val minMessages: Int,
    val mockable: Boolean,
) {
    fun violations(): List<String> = listOfNotNull(
        "metrics.$key: 엔진이 계산하지 않는 지표다".takeIf { DossierMetric.ofKey(key) == null },
        "metrics.$key: p10 < p50 < p90이어야 한다".takeUnless { p10 < p50 && p50 < p90 },
        "metrics.$key: label이 비었다".takeIf { label.isBlank() },
        "metrics.$key: min_messages는 0 이상이다".takeIf { minMessages < 0 },
        "metrics.$key: 모르는 unit $unit".takeUnless { unit in UNITS },
    )

    /** [value]를 [unit]에 맞춰 단위까지 붙인 글. */
    fun format(value: Double): String = when (unit) {
        PER_HUNDRED -> PER_HUNDRED_PREFIX + DossierFormat.decimal(value, PIECES)
        RATIO -> DossierFormat.percent(value * PERCENT)
        CHARACTERS -> DossierFormat.decimal(value, CHARACTERS_UNIT)
        MINUTES -> DossierFormat.minutes(value.toInt())
        HOUR -> DossierFormat.hour(value.toInt())
        DAYS -> DossierFormat.amount(value, DAYS_UNIT)
        TIMES -> DossierFormat.decimal(value, TIMES_UNIT)
        else -> DossierFormat.decimal(value, PIECES)
    }

    /** [value]의 백분위(0~100). p10 아래와 p90 위는 가까운 구간의 기울기로 늘린 뒤 자른다. */
    fun percentileOf(value: Double): Double {
        val raw = if (value <= p50) {
            LOW_ANCHOR + (value - p10) * SPAN / (p50 - p10)
        } else {
            MIDDLE + (value - p50) * SPAN / (p90 - p50)
        }
        return raw.coerceIn(0.0, MAX_PERCENTILE)
    }

    /** [stats]에서 이 지표를 읽는다. 엔진이 모르는 지표이거나 메시지가 모자라거나 값이 없으면 null이다. */
    fun readingOf(stats: DossierStats): MetricReading? {
        val metric = DossierMetric.ofKey(key) ?: return null
        if (stats.messageCount() < minMessages) return null
        val value = metric.valueIn(stats) ?: return null
        return MetricReading(metric, this, value, percentileOf(value))
    }

    companion object {
        private const val LOW_ANCHOR = 10.0
        private const val MIDDLE = 50.0
        private const val SPAN = 40.0
        private const val MAX_PERCENTILE = 100.0
        private const val PERCENT = 100.0
        private const val PER_HUNDRED = "per100"
        private const val RATIO = "ratio"
        private const val COUNT = "count"
        private const val CHARACTERS = "chars"
        private const val MINUTES = "minutes"
        private const val HOUR = "hour"
        private const val DAYS = "days"
        private const val TIMES = "times"
        private val UNITS = setOf(PER_HUNDRED, RATIO, COUNT, CHARACTERS, MINUTES, HOUR, DAYS, TIMES)
        private const val PER_HUNDRED_PREFIX = "100개당 "
        private const val PIECES = "개"
        private const val CHARACTERS_UNIT = "자"
        private const val DAYS_UNIT = "일"
        private const val TIMES_UNIT = "배"
    }
}
