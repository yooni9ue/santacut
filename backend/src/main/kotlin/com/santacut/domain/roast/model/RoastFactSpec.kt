package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.dossier.model.DossierMetric
import com.santacut.domain.dossier.model.MetricReading

/**
 * 조서 지표 하나를 작가 사실로 옮기는 규칙(roast/facts.json의 facts).
 *
 * @property levels 백분위 수준 말 네 개. 순서는 훨씬 낮음, 낮음, 높음, 훨씬 높음이다.
 * @property chipLabel 카드 칩 이름
 */
data class RoastFactSpec(
    val key: String,
    val meaning: String,
    val group: String,
    @JsonProperty("chip_label") val chipLabel: String,
    val levels: List<String>,
) {
    fun violations(): List<String> = listOfNotNull(
        "facts.$key: 엔진이 계산하지 않는 지표다".takeIf { DossierMetric.ofKey(key) == null },
        "facts.$key: levels는 ${LEVEL_COUNT}개다".takeUnless { levels.size == LEVEL_COUNT },
        "facts.$key: meaning·group·chip_label이 비었다".takeIf { meaning.isBlank() || group.isBlank() || chipLabel.isBlank() },
    )

    /** [reading]을 사실로 바꾼다. 백분위가 가운데([BLAND_LOW]~[BLAND_HIGH])라 밋밋하면 null이다. */
    fun factOf(reading: MetricReading): RoastFact? = levelOf(reading.percentileRank())?.let { level ->
        RoastFact(key, meaning, level, group, RoastChip(chipLabel, reading.formattedValue()), reading.directionKey())
    }

    private fun levelOf(percentile: Double): String? = when {
        percentile >= VERY_HIGH -> levels[VERY_HIGH_INDEX]
        percentile > BLAND_HIGH -> levels[HIGH_INDEX]
        percentile <= VERY_LOW -> levels[VERY_LOW_INDEX]
        percentile < BLAND_LOW -> levels[LOW_INDEX]
        else -> null
    }

    companion object {
        private const val LEVEL_COUNT = 4
        private const val VERY_LOW_INDEX = 0
        private const val LOW_INDEX = 1
        private const val HIGH_INDEX = 2
        private const val VERY_HIGH_INDEX = 3
        private const val VERY_HIGH = 90.0
        private const val VERY_LOW = 10.0

        /** 백분위가 이 사이면 남들과 비슷해 놀릴 거리가 못 된다(ㅠ 밀도 0.5처럼 작은 값 포함). */
        private const val BLAND_LOW = 35.0
        private const val BLAND_HIGH = 65.0
    }
}
