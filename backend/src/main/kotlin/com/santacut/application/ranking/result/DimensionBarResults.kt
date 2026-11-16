package com.santacut.application.ranking.result

import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension

/**
 * 결과 화면의 항목 막대 4개. [ScoreDimension] 순서다.
 */
data class DimensionBarResults(val values: List<DimensionBarResult>) {
    companion object {
        fun from(components: ScoreComponents): DimensionBarResults =
            DimensionBarResults(ScoreDimension.entries.map { dimension -> DimensionBarResult.of(dimension, components) })
    }
}
