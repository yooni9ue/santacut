package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 구간 하나에서 읽은 항목 하나의 점수, 기회 수, 근거 줄 번호.
 *
 * @property opportunity 그 항목 행동을 보일 기회 수. 0 없음, 1 한두 번, 2 여러 번이다.
 * @property evidence 범위 밖·겹친 번호를 지운 근거 줄 번호
 * @property selfTurns 그 구간(겹친 앞 줄 제외)의 본인 턴 수
 */
data class DimensionReading(
    val dimension: ScoreDimension,
    val score: BigDecimal,
    val opportunity: Int,
    val evidence: List<Int>,
    val selfTurns: Int,
) {
    fun hasEvidence(): Boolean = evidence.isNotEmpty()

    /** 구간끼리 합칠 때의 무게. 본인 턴이 많고 기회가 많은 구간을 더 믿는다. */
    fun aggregationWeight(): BigDecimal = BigDecimal(selfTurns.coerceAtLeast(1) * (opportunity + 1))
}
