package com.santacut.domain.dossier.model

/**
 * 극값 지표의 계열. 극값 장은 같은 계열에서 하나만 고른다(ㅋ 밀도와 ㅋ 연타가 함께 나오지 않게).
 */
enum class MetricFamily {
    LAUGH,
    CRY,
    EMOJI,
    CLOCK,
    REPLY,
    LENGTH,
    BURST,
    PUNCTUATION,
    VOLUME,
    KIND,
    BLEEP,
}
