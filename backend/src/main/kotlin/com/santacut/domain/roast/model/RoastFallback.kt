package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 작가 문장이 검증을 끝내 통과하지 못했을 때 쓰는 서버 대체 문장(roast/fallback.json).
 * 칭호·판결·담백 판결·쪽지는 판정별, 짝은 사실의 방향 key별(입버릇은 phrase), 줄거리는 막별이다.
 *
 * @property pairs 사실 방향 key별 대체 짝
 * @property genericPairs 쓸 사실이 모자랄 때 채우는 일반 짝
 * @property story 막 번호("1"~"3")별 줄거리 후보
 */
data class RoastFallback(
    val titles: VerdictLines,
    val verdicts: VerdictLines,
    @JsonProperty("verdicts_mild") val verdictsMild: VerdictLines,
    val letters: VerdictLines,
    val pairs: Map<String, FallbackPair>,
    @JsonProperty("generic_pairs") val genericPairs: FallbackPairs,
    val story: Map<String, List<String>>,
) {
    fun violations(): List<String> = listOfNotNull(
        "fallback: 판정별 후보가 빈 목록이 있다".takeIf { titles.isMalformed() || verdicts.isMalformed() || verdictsMild.isMalformed() || letters.isMalformed() },
        "fallback.generic_pairs: 최소 ${RoastSheet.MIN_PAIRS}개가 있어야 한다".takeIf { genericPairs.toList().size < RoastSheet.MIN_PAIRS },
        "fallback.story: 막마다 후보가 있어야 한다".takeUnless { story.keys.containsAll(STORY_KEYS) && emptyList<String>() !in story.values },
    )

    /** [fact]의 대체 짝. 그 방향의 문장이 없으면 null이다. */
    fun pairFor(fact: RoastFact): RoastPair? = pairs[fact.fallbackKey]?.toPair(fact.key, fact.quote)

    /** [act]막 대체 줄거리. */
    fun storyLine(act: Int, seed: Int): RoastStoryLine {
        val candidates = story[act.toString()].orEmpty()
        return RoastStoryLine(act, candidates[seed.mod(candidates.size)], RoastSource.FALLBACK)
    }

    companion object {
        /** 사실 없이 쓴 일반 대체 짝의 사실 key. 칩이 없다. */
        const val GENERIC_KEY = "generic"
        private val STORY_KEYS = setOf("1", "2", "3")
    }
}
