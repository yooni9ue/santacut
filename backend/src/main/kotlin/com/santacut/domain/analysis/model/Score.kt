package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.math.BigDecimal

/**
 * 항목별 점수와 가중 합계. 합계는 100분의 1점 단위 정수다.
 * 검증과 가중 합산은 [ScoreComponents]가 한다.
 */
class Score private constructor(
    val hundredths: Int,
    val components: ScoreComponents,
    val lowConfidence: LowConfidenceDimensions,
) {
    fun verdict(): ScoreVerdict = ScoreVerdict.of(hundredths)

    companion object {
        /** [ScoreDimension] 순서의 항목 점수로 가중 합계를 낸다. [lowConfidence]는 근거가 부족해 중립 점수를 준 항목이다. */
        fun weighted(components: List<BigDecimal>, lowConfidence: LowConfidenceDimensions = LowConfidenceDimensions.NONE): Score {
            val scoreComponents = ScoreComponents(components)
            if (!scoreComponents.isValid()) throw SantaCutException(ErrorCode.AI_SCORE_OUT_OF_RANGE)
            return Score(scoreComponents.weightedHundredths(), scoreComponents, lowConfidence)
        }
    }
}
