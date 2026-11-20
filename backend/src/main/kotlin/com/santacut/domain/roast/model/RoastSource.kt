package com.santacut.domain.roast.model

/** 저장한 팩폭 문장이 어디서 왔는지. AI는 검증을 통과한 작가 문장, FALLBACK은 서버 대체 문장이다. */
enum class RoastSource {
    AI,
    FALLBACK,
}
