package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 공유 링크에 저장한 줄거리 카드의 막 목록(1막부터). JSON에서는 [RecapDeckAct] 배열 하나로 저장한다.
 */
data class RecapDeckActs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @field:JsonValue private val values: List<RecapDeckAct>,
) {
    /** 저장한 순서대로 하나씩 [convert]로 바꾼다. 화면에 그릴 값을 만들 때 쓴다. */
    fun <T> convertEach(convert: (RecapDeckAct) -> T): List<T> = values.map(convert)
}
