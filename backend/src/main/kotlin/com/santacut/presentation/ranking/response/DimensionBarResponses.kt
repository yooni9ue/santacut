package com.santacut.presentation.ranking.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.ranking.result.DimensionBarResults

/**
 * 결과 화면의 항목 막대 4개. JSON에서는 [DimensionBarResponse] 배열 하나로 나간다.
 */
class DimensionBarResponses(@field:JsonValue private val values: List<DimensionBarResponse>) {
    companion object {
        fun from(results: DimensionBarResults): DimensionBarResponses =
            DimensionBarResponses(results.values.map(DimensionBarResponse::from))
    }
}
