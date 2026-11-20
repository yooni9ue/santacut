package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 사실 없이 쓰는 일반 대체 짝 목록(roast/fallback.json의 generic_pairs). JSON에서는 배열 하나로 읽는다. */
class FallbackPairs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<FallbackPair>,
) {
    @JsonValue
    fun toList(): List<FallbackPair> = values.toList()

    /** 일반 대체 짝 [count]개. 사실 key는 [RoastFallback.GENERIC_KEY]다. 준비한 수보다 많이 달라면 있는 만큼만 준다. */
    fun take(count: Int): List<RoastPair> = values.take(count).map { pair -> pair.toPair(RoastFallback.GENERIC_KEY, null) }
}
