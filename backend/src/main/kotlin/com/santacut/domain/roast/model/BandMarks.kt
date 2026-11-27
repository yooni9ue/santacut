package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.analysis.model.GiftGrade

/** 구간이 가진 표현 목록(roast/punch.json의 band_marks). JSON에서는 배열 하나로 읽고 쓴다. */
class BandMarks @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<BandMark>,
) {
    @JsonValue
    fun toList(): List<BandMark> = values.toList()

    /** 빈 표현이거나 모르는 등급을 쓴 항목의 위반. */
    fun violations(): List<String> = values.flatMap { owned ->
        listOfNotNull(
            "punch.band_marks: 빈 표현이 있다".takeIf { owned.mark.isBlank() || owned.bands.isEmpty() },
            (owned.bands - GRADES).takeIf(List<String>::isNotEmpty)?.let { unknown -> "punch.band_marks.${owned.mark}: 모르는 등급 $unknown" },
        )
    }

    /** [grade] 구간이 쓸 수 없는 다른 구간의 표현. */
    fun foreignTo(grade: GiftGrade): ForeignMarks = ForeignMarks(values.filterNot { owned -> grade.name in owned.bands }.map(BandMark::mark))

    companion object {
        val EMPTY = BandMarks(emptyList())
        private val GRADES = GiftGrade.entries.map(GiftGrade::name)
    }
}
