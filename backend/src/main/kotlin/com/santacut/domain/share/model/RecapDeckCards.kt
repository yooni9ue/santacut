package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 공유 링크에 저장한 리캡 카드 목록. 저장한 순서가 곧 화면 순서다. JSON에서는 [RecapDeckCard] 배열 하나로 저장한다.
 */
data class RecapDeckCards @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @field:JsonValue private val values: List<RecapDeckCard>,
) {
    /** 저장한 순서대로 하나씩 [convert]로 바꾼다. 화면에 그릴 값을 만들 때 쓴다. */
    fun <T> convertEach(convert: (RecapDeckCard) -> T): List<T> = values.map(convert)
}
