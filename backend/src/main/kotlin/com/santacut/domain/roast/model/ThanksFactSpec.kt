package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.dossier.model.DossierFormat
import com.santacut.domain.dossier.model.DossierStats

/**
 * 고맙다는 말 횟수([com.santacut.domain.dossier.model.WordStats.thanksCount])를 작가 사실로 옮기는 규칙(roast/facts.json의 thanks).
 * 한 번도 없을 때만 사실이 되고, 점수 깎인 말 카드의 칩(`'고마워' 0번`)으로 쓴다. 말풍선이 적은 사람은 0이 흔해 [minMessages]개부터 본다.
 *
 * @property chipLabel 칩 이름(`'고마워'`)
 * @property minMessages 사실로 쓰려면 필요한 본인 글 메시지 수
 * @property words 고맙다는 말의 줄기. 고마워 칩이 있으면 판정 한 줄이 이 말로 덱을 이끌지 못하게 한다([RoastBrief.thanksWordIn]).
 * @property dimension 고마워 칩이 가리키는 항목 점수 key. 근거 카드 영역 태그에 쓴다.
 */
data class ThanksFactSpec(
    val group: String,
    val meaning: String,
    val level: String,
    @JsonProperty("chip_label") val chipLabel: String,
    @JsonProperty("min_messages") val minMessages: Int,
    val words: List<String> = emptyList(),
    val dimension: String? = null,
) {
    fun violations(): List<String> = listOfNotNull(
        "facts.thanks: group·meaning·level·chip_label이 비었다".takeIf { listOf(group, meaning, level, chipLabel).any(String::isBlank) },
        "facts.thanks: min_messages는 0 이상이다".takeIf { minMessages < 0 },
        "facts.thanks: words가 비었다".takeIf { words.isEmpty() },
        "facts.thanks: 모르는 dimension $dimension".takeIf { dimension != null && ScoreDimension.ofKey(dimension) == null },
    )

    /** [stats]의 고맙다는 말이 한 번도 없으면 그 사실. 횟수가 없는 옛 집계이거나 한 번이라도 있거나 말풍선이 적으면 null이다. */
    fun factOf(stats: DossierStats): RoastFact? {
        if (stats.words.thanksCount != NONE || stats.textCount() < minMessages) return null
        return RoastFact(KEY, meaning, level, group, RoastChip(chipLabel, DossierFormat.times(NONE)), direction = ChipDirection.BAD, dimension = dimension)
    }

    companion object {
        /** 칩 후보 key. 점수 깎인 말 통계 카드가 먼저 고른다. */
        const val KEY = "thanks_count"
        private const val NONE = 0
    }
}
