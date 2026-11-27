package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.dossier.model.DossierFormat
import com.santacut.domain.dossier.model.PhraseEntry
import com.santacut.domain.dossier.model.PhraseTally
import kotlin.math.ln

/**
 * 입버릇 하나를 작가 사실로 옮기는 규칙(roast/facts.json의 phrase). 뜻과 칩 이름의 `{quote}`에 입버릇 표기를 넣는다.
 *
 * @property levels 특이도 수준 말 두 개. 순서는 많음, 훨씬 많음이다.
 * @property weight 편집 무게(기본 1). 입버릇이 지표 사실을 밀어내지 않게 낮출 수 있다.
 * @property minCount 칩으로 쓰려면 필요한 최소 횟수. 이보다 적게 쓴 입버릇은 칩 값이 뜻이 없어 사실로 쓰지 않는다.
 * @property praise 칭찬 입버릇 key('와', '대박', '굳'). 좋은 쪽([ChipDirection.GOOD])이라 점수 깎인 말 근거로 쓰지 않는다.
 * @property negative 나쁜 쪽 입버릇 key('귀찮', '빨리'). 선물 받는 이유·살아남은 말 근거로 쓰지 않는다. 나머지 입버릇은 어느 쪽으로도 쓴다.
 * @property praiseDimension 칭찬 입버릇이 가리키는 항목 점수 key. 근거 카드 영역 태그에 쓴다.
 */
data class PhraseFactSpec(
    val group: String,
    val meaning: String,
    @JsonProperty("chip_label") val chipLabel: String,
    val levels: List<String>,
    val weight: Double = RoastFactSpec.DEFAULT_WEIGHT,
    @JsonProperty("min_count") val minCount: Int = 0,
    val praise: List<String> = emptyList(),
    val negative: List<String> = emptyList(),
    @JsonProperty("praise_dimension") val praiseDimension: String? = null,
) {
    fun violations(): List<String> = listOfNotNull(
        "facts.phrase: levels는 ${LEVEL_COUNT}개다".takeUnless { levels.size == LEVEL_COUNT },
        "facts.phrase: meaning과 chip_label에 {quote}가 있어야 한다".takeUnless { QUOTE in meaning && QUOTE in chipLabel },
        "facts.phrase: weight는 0보다 크고 ${MAX_WEIGHT} 이하다".takeUnless { weight > 0.0 && weight <= MAX_WEIGHT },
        "facts.phrase: praise와 negative에 함께 든 key ${praise.intersect(negative.toSet())}".takeIf { praise.any(negative::contains) },
        "facts.phrase: 모르는 praise_dimension $praiseDimension".takeIf { praiseDimension != null && ScoreDimension.ofKey(praiseDimension) == null },
    )

    /** 입버릇 [phraseKey]의 방향. 칭찬 입버릇은 좋은 쪽, 나쁜 입버릇은 나쁜 쪽, 나머지는 어느 쪽으로도 쓴다. */
    fun directionOf(phraseKey: String): ChipDirection = when (phraseKey) {
        in praise -> ChipDirection.GOOD
        in negative -> ChipDirection.BAD
        else -> ChipDirection.EITHER
    }

    /**
     * 본인 메시지 [messageCount]개 중 [tally]만큼 쓴 입버릇 [entry]의 사실. 보통 사람보다 드물게 튀지 않거나 [minCount]번보다 적으면 null이다.
     * 점수는 특이도를 [FULL_SCORE](보통의 여섯 배)에서 1이 되게 줄인 값 × [weight]다.
     */
    fun factOf(tally: PhraseTally, entry: PhraseEntry, messageCount: Int): RoastFact? {
        if (messageCount <= 0 || tally.count < minCount) return null
        val specificity = entry.specificity(tally.count, messageCount)
        val level = when {
            specificity >= MUCH_MORE -> levels[MUCH_MORE_INDEX]
            specificity >= MORE -> levels[MORE_INDEX]
            else -> return null
        }
        val chip = RoastChip(chipLabel.replace(QUOTE, tally.display), DossierFormat.times(tally.count))
        val score = minOf(specificity / FULL_SCORE, 1.0) * weight
        val direction = directionOf(tally.key)
        return RoastFact(
            KEY_PREFIX + tally.key, meaning.replace(QUOTE, tally.display), level, group, chip, score,
            direction = direction, dimension = praiseDimension.takeIf { direction == ChipDirection.GOOD },
        )
    }

    companion object {
        const val QUOTE = "{quote}"
        /** 입버릇 사실 key 머리말. 뒤에 입버릇 key가 붙는다. */
        const val KEY_PREFIX = "phrase_"
        private const val LEVEL_COUNT = 2
        private const val MORE_INDEX = 0
        private const val MUCH_MORE_INDEX = 1

        /** 보통 사람보다 1.5배 넘게 쓰면 많음, 3배 넘게 쓰면 훨씬 많음이다(특이도는 로그 비율). */
        private val MORE = ln(1.5)
        private val MUCH_MORE = ln(3.0)
        private val FULL_SCORE = ln(6.0)
        private const val MAX_WEIGHT = 2.0
    }
}
