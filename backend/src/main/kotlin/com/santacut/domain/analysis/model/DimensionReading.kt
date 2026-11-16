package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 구간 하나에서 읽은 항목 하나의 점수, 기회 수, 근거 줄 번호.
 *
 * @property opportunity 그 항목 행동을 보일 기회 수. 0 없음, 1 한두 번, 2 여러 번이다.
 * @property evidence 범위 밖·겹친 번호를 지운 근거 줄 번호
 * @property turnWeight 그 구간의 본인 턴 무게([ScoringChunk.turnWeight])
 */
data class DimensionReading(
    val dimension: ScoreDimension,
    val score: BigDecimal,
    val opportunity: Int,
    val evidence: List<Int>,
    val turnWeight: Int,
) {
    fun hasEvidence(): Boolean = evidence.isNotEmpty()

    /** 구간끼리 합칠 때의 무게. 본인 턴이 많고 기회가 많은 구간을 더 믿는다. 기회가 0이어도 근거가 있으면 무게가 남는다. */
    fun aggregationWeight(): BigDecimal = BigDecimal(turnWeight * (opportunity + BASE_OPPORTUNITY_WEIGHT))

    companion object {
        /** 기회 수에 더하는 기본 무게. 기회 수 0인 근거도 0으로 사라지지 않게 한다. */
        private const val BASE_OPPORTUNITY_WEIGHT = 1
    }
}
