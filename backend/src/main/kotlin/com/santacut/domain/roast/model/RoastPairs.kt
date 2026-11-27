package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 작가 프롬프트 1~4판으로 저장한 옛 팩폭 결과의 놀림·뒤집기 짝 목록. 읽기 전용이다.
 * 새 리캡 덱에는 짝 문장을 쓰지 않고 짝에 붙은 서버 숫자 칩만 통계 카드로 옮긴다([RoastContent.upgraded]). JSON에서는 배열 하나로 읽는다.
 */
class RoastPairs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<RoastPair>,
) {
    @JsonValue
    fun toList(): List<RoastPair> = values.toList()

    /** 사실 key와 칩이 있는 짝의 칩 후보. 사실 없이 쓴 대체 짝(칩 없음)은 뺀다. */
    fun chipFacts(): RoastChipFacts = RoastChipFacts(values.mapNotNull { pair -> pair.chip?.let { chip -> RoastChipFact(pair.factKey, chip) } })
}
