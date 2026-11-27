package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.dossier.model.DossierFormat
import com.santacut.domain.dossier.model.DossierStats

/**
 * 고맙다는 말 횟수([com.santacut.domain.dossier.model.WordStats.thanksCount])를 작가 사실로 옮기는 규칙(roast/facts.json의 thanks).
 * 한 번도 없을 때만 사실이 되고, 점수 깎인 말 카드의 칩(`'고마워' 0번`)으로 쓴다. 말풍선이 적은 사람은 0이 흔해 [minMessages]개부터 본다.
 *
 * @property chipLabel 칩 이름(`'고마워'`)
 * @property minMessages 사실로 쓰려면 필요한 본인 글 메시지 수
 */
data class ThanksFactSpec(
    val group: String,
    val meaning: String,
    val level: String,
    @JsonProperty("chip_label") val chipLabel: String,
    @JsonProperty("min_messages") val minMessages: Int,
) {
    fun violations(): List<String> = listOfNotNull(
        "facts.thanks: group·meaning·level·chip_label이 비었다".takeIf { listOf(group, meaning, level, chipLabel).any(String::isBlank) },
        "facts.thanks: min_messages는 0 이상이다".takeIf { minMessages < 0 },
    )

    /** [stats]의 고맙다는 말이 한 번도 없으면 그 사실. 횟수가 없는 옛 집계이거나 한 번이라도 있거나 말풍선이 적으면 null이다. */
    fun factOf(stats: DossierStats): RoastFact? {
        if (stats.words.thanksCount != NONE || stats.textCount() < minMessages) return null
        return RoastFact(KEY, meaning, level, group, RoastChip(chipLabel, DossierFormat.times(NONE)))
    }

    companion object {
        /** 칩 후보 key. 점수 깎인 말 통계 카드가 먼저 고른다. */
        const val KEY = "thanks_count"
        private const val NONE = 0
    }
}
