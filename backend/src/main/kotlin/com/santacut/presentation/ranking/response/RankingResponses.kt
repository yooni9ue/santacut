package com.santacut.presentation.ranking.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.ranking.result.RankingRowResults

/**
 * 순위표 페이지 응답. JSON에서는 [RankingResponse] 배열 하나로 나간다.
 */
class RankingResponses(@field:JsonValue private val values: List<RankingResponse>) {
    companion object {
        fun from(results: RankingRowResults): RankingResponses = RankingResponses(results.map(RankingResponse::from))
    }
}
