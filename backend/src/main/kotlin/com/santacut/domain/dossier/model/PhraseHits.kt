package com.santacut.domain.dossier.model

/**
 * 본인 메시지에서 한 번 이상 나온 허용 어휘 목록. 많이 쓴 말과 남들보다 유독 많이 쓴 말(특이도)을 고른다.
 * 횟수나 특이도가 같으면 key 순서로 정해 늘 같은 결과를 낸다.
 */
class PhraseHits(private val values: List<PhraseHit>) {
    /** 많이 쓴 말 상위 [FREQUENT_LIMIT]개. [MIN_FREQUENT_COUNT]번 이상 쓴 말만. */
    fun frequent(): PhraseTallies = PhraseTallies(
        values.filter { hit -> hit.count >= MIN_FREQUENT_COUNT }
            .sortedWith(compareByDescending<PhraseHit> { hit -> hit.count }.thenBy { hit -> hit.key })
            .take(FREQUENT_LIMIT)
            .map(PhraseHit::toTally),
    )

    /** 특이도 상위 [DISTINCT_LIMIT]개. 메시지 [messageCount]개 기준 최소 횟수(max(5, 메시지 수/200))를 넘은 말만. */
    fun distinct(messageCount: Int): PhraseTallies {
        if (messageCount == 0) return PhraseTallies(emptyList())
        val minimum = maxOf(MIN_DISTINCT_COUNT, messageCount / DISTINCT_DIVISOR)
        return PhraseTallies(
            values.filter { hit -> hit.count >= minimum }
                .sortedWith(compareByDescending<PhraseHit> { hit -> hit.specificity(messageCount) }.thenBy { hit -> hit.key })
                .take(DISTINCT_LIMIT)
                .map(PhraseHit::toTally),
        )
    }

    companion object {
        const val FREQUENT_LIMIT = 5
        const val MIN_FREQUENT_COUNT = 3
        const val DISTINCT_LIMIT = 3
        const val MIN_DISTINCT_COUNT = 5
        const val DISTINCT_DIVISOR = 200
    }
}
