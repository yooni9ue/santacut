package com.santacut.application.ranking.result

import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import java.math.BigDecimal

/**
 * 결과 화면 막대 하나: 채점 항목 키, 공개 이름, 항목 점수(0~100).
 */
data class DimensionBarResult(
    val key: String,
    val label: String,
    val score: BigDecimal,
) {
    companion object {
        fun of(dimension: ScoreDimension, components: ScoreComponents): DimensionBarResult =
            DimensionBarResult(dimension.key, dimension.label, components.scoreOf(dimension))
    }
}
