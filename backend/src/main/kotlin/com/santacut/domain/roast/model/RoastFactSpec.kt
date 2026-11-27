package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.dossier.model.DossierMetric
import com.santacut.domain.dossier.model.MetricReading

/**
 * 조서 지표 하나를 작가 사실로 옮기는 규칙(roast/facts.json의 facts).
 *
 * @property levels 백분위 수준 말 네 개. 순서는 훨씬 낮음, 낮음, 높음, 훨씬 높음이다.
 * @property chipLabel 카드 칩 이름
 * @property weight 편집 무게(기본 1). 메시지 양에 따라 늘 튀는 지표(말풍선 수, 운행 일수, 최대 연타 등)는 낮춰 덜 뽑히게 한다.
 * @property quoteRule 이 칩 근거 카드에 붙여도 되는 말풍선 조건(높음 수준일 때). 말풍선이 그 칩이 센 것의 실례일 때만(센 글자·물음표가 든 말풍선) 조건을 둔다.
 * 시간대·요일·묶음 비율처럼 글자로 보이지 않는 지표는 [QuoteRule.NONE]이다. 낮음 수준 사실은 늘 [QuoteRule.NONE]으로 쓴다.
 * @property directions 수준마다의 방향([ChipDirection]). 순서는 [levels]와 같다. 중립 수준 사실은 근거 카드에 쓰지 않는다.
 * @property dimension 좋은 쪽·나쁜 쪽 방향이 가리키는 항목 점수 key([ScoreDimension.key]). 근거 카드 영역 태그에 쓴다. 이어 볼 항목이 없으면 null이다.
 * @property topics 칩이 말하는 버릇의 낱말. 근거 한 줄이 칩 글자를 그대로 옮기지 않고 뜻으로 말해도 근거와 겹친 것으로 친다([TextProblems.grounding]).
 * @property minValue 칩 값이 이보다 작으면 뜻이 없어(연달아 보내기 한 개 등) 사실로 쓰지 않는다. 없으면 null이다.
 * @property cutMaxValue 나쁜 쪽(−) 방향을 쓰는 값의 상한. 값이 이보다 크면 그 수준이 −여도 중립으로 본다. ㅋ 밀도가 열 번에 한 번보다 잦으면
 * 웃음이 적다고 깎지 않는다('3번에 1번'에 노웃음상이 붙던 뒤집힘). 없으면 null이다.
 */
data class RoastFactSpec(
    val key: String,
    val meaning: String,
    val group: String,
    @JsonProperty("chip_label") val chipLabel: String,
    val levels: List<String>,
    @JsonProperty("quote_rule") val quoteRule: QuoteRule,
    val weight: Double = DEFAULT_WEIGHT,
    val directions: List<ChipDirection> = emptyList(),
    val dimension: String? = null,
    val topics: List<String> = emptyList(),
    @JsonProperty("min_value") val minValue: Double? = null,
    @JsonProperty("cut_max_value") val cutMaxValue: Double? = null,
) {
    fun violations(): List<String> = listOfNotNull(
        "facts.$key: 엔진이 계산하지 않는 지표다".takeIf { DossierMetric.ofKey(key) == null },
        "facts.$key: levels는 ${LEVEL_COUNT}개다".takeUnless { levels.size == LEVEL_COUNT },
        "facts.$key: meaning·group·chip_label이 비었다".takeIf { meaning.isBlank() || group.isBlank() || chipLabel.isBlank() },
        "facts.$key: weight는 0보다 크고 ${MAX_WEIGHT} 이하다".takeUnless { weight > 0.0 && weight <= MAX_WEIGHT },
        "facts.$key: 지표 칩에는 입버릇 말풍선 조건(phrase)을 쓰지 않는다".takeIf { quoteRule == QuoteRule.PHRASE },
        "facts.$key: 지표 칩 말풍선 조건은 센 글자가 보이는 것(question·exclaim·laugh·kind)이나 none이다".takeUnless { quoteRule in TOKEN_RULES },
        "facts.$key: directions는 ${LEVEL_COUNT}개다".takeUnless { directions.size == LEVEL_COUNT },
        "facts.$key: directions에 ±는 쓰지 않는다".takeIf { ChipDirection.EITHER in directions },
        "facts.$key: 모르는 dimension $dimension".takeIf { dimension != null && ScoreDimension.ofKey(dimension) == null },
        "facts.$key: cut_max_value는 − 방향이 있을 때만 쓴다".takeIf { cutMaxValue != null && ChipDirection.BAD !in directions },
    )

    /**
     * [reading]을 사실로 바꾼다. 백분위가 가운데([BLAND_LOW]~[BLAND_HIGH])라 밋밋하거나 값이 [minValue]보다 작으면 null이다.
     * 점수는 튀는 정도 × [weight]이고, 방향은 그 수준의 [directions]다. 나쁜 쪽인데 값이 [cutMaxValue]보다 크면 중립이다.
     * 칩 값은 읽기 쉬운 꼴([MetricReading.chipValue])로 찍는다.
     */
    fun factOf(reading: MetricReading): RoastFact? {
        if (minValue != null && reading.rawValue() < minValue) return null
        val index = levelIndexOf(reading.percentileRank()) ?: return null
        val chip = RoastChip(chipLabel, reading.chipValue())
        return RoastFact(
            key, meaning, levels[index], group, chip, extremenessOf(reading.percentileRank()) * weight, quoteRuleAt(reading.percentileRank()),
            directionAt(index, reading.rawValue()), dimension, topics,
        )
    }

    /** [index] 수준에서 값 [value]의 방향. 나쁜 쪽인데 값이 [cutMaxValue]보다 크면 깎을 만큼 튀지 않아 중립이다. */
    private fun directionAt(index: Int, value: Double): ChipDirection {
        val direction = directions.getOrElse(index) { ChipDirection.NEUTRAL }
        val mild = direction == ChipDirection.BAD && cutMaxValue != null && value > cutMaxValue
        return if (mild) ChipDirection.NEUTRAL else direction
    }

    /**
     * 백분위 [percentile]에서의 말풍선 조건. 낮음·훨씬 낮음 수준이면 버릇이 드물다는 칩이라 그 버릇이 든 말풍선을 요구할 수 없어
     * 말풍선 없이 쓴다([QuoteRule.NONE]). '새벽 출몰률이 낮다' 칩에 새벽 말풍선을 붙이라고 하지 않으려는 것이다.
     */
    private fun quoteRuleAt(percentile: Double): QuoteRule = if (percentile < BLAND_LOW) QuoteRule.NONE else quoteRule

    private fun levelIndexOf(percentile: Double): Int? = when {
        percentile >= VERY_HIGH -> VERY_HIGH_INDEX
        percentile > BLAND_HIGH -> HIGH_INDEX
        percentile <= VERY_LOW -> VERY_LOW_INDEX
        percentile < BLAND_LOW -> LOW_INDEX
        else -> null
    }

    companion object {
        const val DEFAULT_WEIGHT = 1.0
        private const val MAX_WEIGHT = 2.0
        private const val MIDDLE = 50.0
        private const val MAX_PERCENTILE = 100.0
        private const val LEVEL_COUNT = 4
        private const val VERY_LOW_INDEX = 0
        private const val LOW_INDEX = 1
        private const val HIGH_INDEX = 2
        private const val VERY_HIGH_INDEX = 3
        private const val VERY_HIGH = 90.0
        private const val VERY_LOW = 10.0

        /** 지표 칩이 쓸 수 있는 말풍선 조건. 말풍선에 센 글자(물음표·느낌표·ㅋ·따뜻한 말)가 보이는 것과 말풍선 없음뿐이다. */
        private val TOKEN_RULES = setOf(QuoteRule.QUESTION, QuoteRule.EXCLAIM, QuoteRule.LAUGH, QuoteRule.KIND, QuoteRule.NONE)

        /** 백분위가 이 사이면 남들과 비슷해 놀릴 거리가 못 된다(ㅠ 밀도 0.5처럼 작은 값 포함). */
        private const val BLAND_LOW = 35.0
        private const val BLAND_HIGH = 65.0

        /** 튀는 정도(0~1). 백분위가 50이면 0이고 양 끝(0, 100)으로 갈수록 1에 가깝다. */
        fun extremenessOf(percentile: Double): Double = (MIDDLE - minOf(percentile, MAX_PERCENTILE - percentile)) / MIDDLE
    }
}
