package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.dossier.model.DossierFormat
import com.santacut.domain.dossier.model.PhraseEntry
import com.santacut.domain.dossier.model.PhraseTally
import kotlin.math.ln

/**
 * 입버릇 하나를 작가 사실로 옮기는 규칙(roast/facts.json의 phrase). 뜻과 칩 이름의 `{quote}`에 입버릇 표기를 넣는다.
 *
 * @property levels 특이도 수준 말 두 개. 순서는 많음, 훨씬 많음이다.
 */
data class PhraseFactSpec(
    val group: String,
    val meaning: String,
    @JsonProperty("chip_label") val chipLabel: String,
    val levels: List<String>,
) {
    fun violations(): List<String> = listOfNotNull(
        "facts.phrase: levels는 ${LEVEL_COUNT}개다".takeUnless { levels.size == LEVEL_COUNT },
        "facts.phrase: meaning과 chip_label에 {quote}가 있어야 한다".takeUnless { QUOTE in meaning && QUOTE in chipLabel },
    )

    /** 본인 메시지 [messageCount]개 중 [tally]만큼 쓴 입버릇 [entry]의 사실. 보통 사람보다 드물게 튀지 않으면 null이다. */
    fun factOf(tally: PhraseTally, entry: PhraseEntry, messageCount: Int): RoastFact? {
        if (messageCount <= 0) return null
        val specificity = entry.specificity(tally.count, messageCount)
        val level = when {
            specificity >= MUCH_MORE -> levels[MUCH_MORE_INDEX]
            specificity >= MORE -> levels[MORE_INDEX]
            else -> return null
        }
        val chip = RoastChip(chipLabel.replace(QUOTE, tally.display), DossierFormat.times(tally.count))
        return RoastFact(KEY_PREFIX + tally.key, meaning.replace(QUOTE, tally.display), level, group, chip, FALLBACK_KEY, tally.display)
    }

    companion object {
        const val FALLBACK_KEY = "phrase"
        const val QUOTE = "{quote}"
        private const val KEY_PREFIX = "phrase_"
        private const val LEVEL_COUNT = 2
        private const val MORE_INDEX = 0
        private const val MUCH_MORE_INDEX = 1

        /** 보통 사람보다 1.5배 넘게 쓰면 많음, 3배 넘게 쓰면 훨씬 많음이다(특이도는 로그 비율). */
        private val MORE = ln(1.5)
        private val MUCH_MORE = ln(3.0)
    }
}
