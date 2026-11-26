package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 공유 링크에 저장한 항목 막대 카드의 막대 목록(공감·응원·존중·협력 순서). JSON에서는 [RecapDeckBar] 배열 하나로 저장한다.
 */
data class RecapDeckBars @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @field:JsonValue private val values: List<RecapDeckBar>,
) {
    /** 저장한 순서대로 하나씩 [convert]로 바꾼다. 화면에 그릴 값을 만들 때 쓴다. */
    fun <T> convertEach(convert: (RecapDeckBar) -> T): List<T> = values.map(convert)
}
