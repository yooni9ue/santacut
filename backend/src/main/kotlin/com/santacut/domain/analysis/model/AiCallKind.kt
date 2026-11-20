package com.santacut.domain.analysis.model

/** AI 호출의 종류. 종류마다 모델과 토큰 단가가 다르므로 비용 가드가 따로 센다. */
enum class AiCallKind {
    /** 점수 산정(채점 모델). */
    SCORE,

    /** 「팩폭 산타」 작가(작가 모델). */
    ROAST,
}
