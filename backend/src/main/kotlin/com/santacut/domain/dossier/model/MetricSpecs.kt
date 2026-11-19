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

    /** [key] 지표 값을 그 unit으로 포맷한 글. 사전에 없는 지표면 null이다. */
    fun format(key: String, value: Double): String? = values.find { spec -> spec.key == key }?.format(value)

    /** [stats]에서 읽을 수 있는 지표들. */
    fun readings(stats: DossierStats): MetricReadings = MetricReadings(values.mapNotNull { spec -> spec.readingOf(stats) })
}
