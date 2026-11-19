package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 극값 후보 지표 사전(metrics.json의 metrics). JSON에서는 배열 하나로 읽는다.
 */
class MetricSpecs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<MetricSpec>,
) {
    @JsonValue
    fun toList(): List<MetricSpec> = values.toList()

    /** 항목마다의 위반, 겹친 key, 극값 문구([extremes])에 high·low가 없는 지표. */
    fun violations(extremes: ExtremeCopies): List<String> =
        values.flatMap(MetricSpec::violations) +
            values.groupBy(MetricSpec::key).filterValues { same -> same.size > 1 }.keys.map { key -> "metrics: key $key 중복" } +
            values.filterNot { spec -> extremes.has(spec.key) }.map { spec -> "copy.extremes: ${spec.key}의 high·low 문구가 없다" }

    /**
     * 지표 방향 key 문구(죄목, 주석, 칭호)의 위반. 놀릴 수 없는 지표(mockable 거짓)는 쓸 수 없고,
     * [forCard]면(칭호처럼 늘 공유 카드에 실리는 문구) 카드에 싣지 않는 지표 방향(느린 답장, 잦은 삐 처리)도 쓸 수 없다.
     */
    fun copyViolations(path: String, keys: List<String>, forCard: Boolean): List<String> =
        keys.mapNotNull { key -> copyViolationOf(path, key, forCard) }

    private fun copyViolationOf(path: String, key: String, forCard: Boolean): String? {
        val metricKey = key.removeSuffix(MetricReading.LOW_SUFFIX)
        val spec = values.find { candidate -> candidate.key == metricKey } ?: return null
        if (!spec.mockable) return "$path.$key: 놀릴 수 없는 지표는 쓸 수 없다"
        val metric = DossierMetric.ofKey(metricKey) ?: return null
        return "$path.$key: 공유 카드에 싣지 않는 지표 방향은 쓸 수 없다".takeIf { forCard && !metric.isCardSafe(key == metricKey) }
    }

    /** [key] 지표 값을 그 unit으로 포맷한 글. 사전에 없는 지표면 null이다. */
    fun format(key: String, value: Double): String? = values.find { spec -> spec.key == key }?.format(value)

    /** [stats]에서 읽을 수 있는 지표들. */
    fun readings(stats: DossierStats): MetricReadings = MetricReadings(values.mapNotNull { spec -> spec.readingOf(stats) })
}
