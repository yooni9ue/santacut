package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 사전으로 센 낱말 집계. 입버릇은 허용 어휘만, 욕설은 합산 횟수만 담는다.
 *
 * @property phrasesFrequent 많이 쓴 말 상위 5개(3번 이상)
 * @property phrasesDistinct 특이도 상위 3개
 * @property kindWords 따뜻한 말별 횟수
 * @property topics 주제별 막별 횟수
 * @property bleepCount 일반 욕설 합산 횟수. 낱말은 남기지 않는다.
 * @property thanksCount 고맙다는 말(고마워, 고맙, ㄱㅅ, 감사, 땡큐)이 든 본인 말풍선 수. 이 값이 생기기 전(집계 2판까지) 조서는 null이다.
 */
data class WordStats(
    @JsonProperty("phrases_frequent") val phrasesFrequent: PhraseTallies,
    @JsonProperty("phrases_distinct") val phrasesDistinct: PhraseTallies,
    @JsonProperty("kind_words") val kindWords: KindCounts,
    val topics: TopicActCounts,
    @JsonProperty("bleep_count") val bleepCount: Int,
    @JsonProperty("thanks_count") val thanksCount: Int? = null,
) {
    /** 입버릇 목록을 지금 사전 [entries]로 다시 읽은 집계. */
    fun refreshedBy(entries: PhraseEntries): WordStats =
        copy(phrasesFrequent = phrasesFrequent.refreshedBy(entries), phrasesDistinct = phrasesDistinct.refreshedBy(entries))
}
