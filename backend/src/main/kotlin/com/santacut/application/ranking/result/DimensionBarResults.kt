package com.santacut.application.ranking.result

import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.share.model.RecapDeckBar
import com.santacut.domain.share.model.RecapDeckBars

/**
 * 결과 화면의 항목 막대 4개. [ScoreDimension] 순서다.
 */
data class DimensionBarResults(val values: List<DimensionBarResult>) {
    /** 공유 링크 리캡 카드에 저장할 막대 목록. 이름과 점수만 옮긴다. */
    fun toDeckBars(): RecapDeckBars = RecapDeckBars(values.map { bar -> RecapDeckBar(bar.label, bar.score) })

    companion object {
        fun from(components: ScoreComponents): DimensionBarResults =
            DimensionBarResults(ScoreDimension.entries.map { dimension -> DimensionBarResult.of(dimension, components) })
    }
}
