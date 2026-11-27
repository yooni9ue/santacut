package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents

/**
 * 저장하는 팩폭 결과(roasts.content_json). 리캡 덱의 판정 한 줄, 근거 카드, 결론 카드 문장과 출처를 담는다.
 * 원문은 담지 않고, 근거로 고른 본인 줄만 이름을 가리고 20자 안으로 자른 말풍선 표시로 담는다. 상대 말은 담지 않는다.
 *
 * 작가 프롬프트 1~4판으로 저장한 옛 결과도 읽는다. 그때 필드(title, verdict_mild, letter, story 등)는 모르는 필드로 넘기고,
 * 짝([legacyPairs])의 칩만 [forDeck]이 통계 카드로 옮긴다. 옛 결과에는 [conclusion]이 없다.
 *
 * @property verdict 쓸 때의 판정 key(gift, coal)
 * @property verdictLine 판정 카드의 한 줄
 * @property evidence 근거 카드. 옛 결과면 비어 있다.
 * @property conclusion 결론 카드 문장. 옛 결과면 null이다.
 * @property legacyPairs 옛 결과의 짝. 새 결과는 null이라 저장하지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RoastContent(
    val verdict: String,
    @JsonProperty("verdict_line") val verdictLine: RoastText,
    val evidence: RoastEvidences = RoastEvidences.EMPTY,
    val conclusion: RoastConclusion? = null,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("pairs") val legacyPairs: RoastPairs? = null,
) {
    /** 지금 덱 형식(작가 프롬프트 5판 이후)으로 쓴 결과인지. */
    fun isCurrent(): Boolean = conclusion != null

    /** 지금 형식이고 작가가 쓴 문장이 하나라도 있는지. 모두 대체 문장이거나 옛 결과면 거짓이다. */
    fun hasAiText(): Boolean = isCurrent() && fallbackCount() < 1 + evidence.size() + RoastConclusion.TEXTS

    /** 대체 문장으로 채운 문장 수. 로그에는 이 수와 필드 이름만 남긴다. */
    fun fallbackCount(): Int = verdictLine.fallbackCount() + evidence.fallbackCount() + (conclusion?.fallbackCount() ?: 0)

    /**
     * 덱에 실을 내용. 옛 결과면 짝의 칩으로 통계 카드를 만들고 구간별 대체 결론을 붙인다.
     * 어느 쪽이든 판정 [gift]의 종류별 상한으로 자르고 항목 점수 [dimensions] 통계 카드로 최소 장수를 채운다.
     */
    fun forDeck(gift: GiftVerdict, dimensions: ScoreComponents, fallback: RoastFallback): RoastContent {
        val current = if (isCurrent()) this else upgraded(gift, fallback)
        return current.copy(evidence = current.evidence.completedFor(gift.passed, dimensions, fallback), legacyPairs = null)
    }

    private fun upgraded(gift: GiftVerdict, fallback: RoastFallback): RoastContent {
        val chips = legacyPairs?.chipFacts() ?: RoastChipFacts.EMPTY
        val evidence = chips.fallbackEvidence(if (gift.passed) LEGACY_GIFT_SLOTS else LEGACY_COAL_SLOTS, fallback)
        return RoastContent(verdict, verdictLine, evidence, fallback.conclusionOf(gift.grade(), evidence.hero(), plain = false))
    }

    companion object {
        private val LEGACY_GIFT_SLOTS = RoastSlots(3, 0, 0)
        private val LEGACY_COAL_SLOTS = RoastSlots(0, 3, 1)

        /** 저장한 JSON을 읽을 수 없을 때 쓰는 구간별 대체 내용. 리캡 조회가 옛 데이터로 실패하지 않게 한다. */
        fun fallbackOf(gift: GiftVerdict, fallback: RoastFallback): RoastContent {
            val band = fallback.bandOf(gift.grade())
            return RoastContent(gift.key(), RoastText(band.verdictLine(plain = false), RoastSource.FALLBACK), RoastEvidences.EMPTY,
                fallback.conclusionOf(gift.grade(), null, plain = false))
        }
    }
}
