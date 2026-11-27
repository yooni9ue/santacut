package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.roast.result.RecapBandResults

/** 컷 표 여섯 칸. 위에서부터 높은 구간 순서다. JSON에서는 배열 하나로 나간다. */
class BandResponses(@field:JsonValue private val values: List<BandResponse>) {
    companion object {
        fun from(bands: RecapBandResults): BandResponses =
            BandResponses(bands.values.map { band -> BandResponse(band.code, band.label, band.range, band.current) })
    }
}
