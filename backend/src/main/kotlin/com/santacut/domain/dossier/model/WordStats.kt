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
 */
data class WordStats(
    @JsonProperty("phrases_frequent") val phrasesFrequent: PhraseTallies,
    @JsonProperty("phrases_distinct") val phrasesDistinct: PhraseTallies,
    @JsonProperty("kind_words") val kindWords: KindCounts,
    val topics: TopicActCounts,
    @JsonProperty("bleep_count") val bleepCount: Int,
)
