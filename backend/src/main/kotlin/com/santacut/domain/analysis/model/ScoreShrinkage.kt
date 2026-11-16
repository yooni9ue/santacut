package com.santacut.domain.analysis.model

import java.math.BigDecimal
import java.math.MathContext

/**
 * 항목 점수를 사전 평균 쪽으로 당기는 규칙. x' = (C·m + w·x) / (C + w)이다.
 * 사전 평균 m은 근거 없는 항목에 주는 중립 점수([DimensionScores.NEUTRAL_SCORE])와 같은 값이다.
 * 근거 무게 w = 기회 수 n(0~2) × min(1, 본인 턴 수 / [fullSelfTurns])다. 기회가 없으면(n=0) 사전 평균 그대로다.
 *
 * @property strength 사전 평균의 무게 C. 0이면 당기지 않는다.
 * @property fullSelfTurns 근거 무게를 깎지 않는 본인 턴 수
 */
data class ScoreShrinkage(
    val strength: BigDecimal,
    val fullSelfTurns: Int,
) {
    /** [score]를 기회 수 [opportunity]와 본인 턴 수 [selfTurns]에 맞춰 당긴 정밀 값. */
    fun shrink(score: BigDecimal, opportunity: Int, selfTurns: Int): BigDecimal {
        val weight = BigDecimal(opportunity) * coverage(selfTurns)
        val denominator = strength + weight
        if (denominator.signum() == 0) return score
        return (strength * DimensionScores.NEUTRAL_SCORE + weight * score).divide(denominator, PRECISION)
    }

    private fun coverage(selfTurns: Int): BigDecimal =
        BigDecimal(selfTurns.coerceIn(0, fullSelfTurns)).divide(BigDecimal(fullSelfTurns), PRECISION)

    companion object {
        val PRECISION: MathContext = MathContext.DECIMAL64
    }
}
