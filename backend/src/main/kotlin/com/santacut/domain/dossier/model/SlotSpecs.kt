package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * copy.json `slots`에 등록한 슬롯 목록. 템플릿은 이 이름만 쓸 수 있다(미등록 슬롯 검사).
 */
class SlotSpecs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<SlotSpec>,
) {
    @JsonValue
    fun toList(): List<SlotSpec> = values.toList()

    fun names(): Set<String> = values.map(SlotSpec::name).toSet()

    /** 금지 슬롯을 등록했으면 위반이다. */
    fun violations(): List<String> =
        values.filter { spec -> spec.name in Slots.FORBIDDEN }.map { spec -> "slots: 금지 슬롯 ${spec.name}을 등록했다" }
}
