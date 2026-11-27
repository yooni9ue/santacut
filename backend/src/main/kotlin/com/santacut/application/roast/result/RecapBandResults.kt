package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.share.model.RecapDeckBand
import com.santacut.domain.share.model.RecapDeckBands

/** 컷 표 여섯 칸. 위에서부터 높은 구간(굴뚝 프리패스) 순서다. */
data class RecapBandResults(val values: List<RecapBandResult>) {
    /** 공유 링크에 저장할 칸 목록. */
    fun toDeckBands(): RecapDeckBands = RecapDeckBands(values.map { band -> RecapDeckBand(band.label, band.range, band.current) })

    companion object {
        /** 내 구간 [current]에 불을 켠 여섯 칸. */
        fun of(current: GiftGrade): RecapBandResults =
            RecapBandResults(GiftGrade.entries.map { grade -> RecapBandResult(grade.name, grade.label, grade.range, grade == current) })
    }
}
