package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * AI 모델이 [ScoreDimension] 순서로 돌려준 항목 점수 목록. 숫자가 아닌 항목은 null이다.
 */
class DimensionScores(private val values: List<BigDecimal?>) {
    /** 모든 항목의 점수가 숫자로 들어왔는지. */
    fun isComplete(): Boolean = values.size == ScoreDimension.entries.size && values.all { it != null }

    /** 들어온 점수가 모두 0~100점이고 소수 둘째 자리까지인지. */
    fun isWithinRange(): Boolean = ScoreComponents(values.filterNotNull()).isValid()

    /** 항목마다 점수, 기회 수, [evidence]의 근거 번호, 구간의 본인 턴 무게를 묶는다. [isComplete]를 통과한 목록에서 부른다. */
    fun toReadings(evidence: EvidenceIndices, opportunities: DimensionOpportunities, turnWeight: Int): DimensionReadings =
        DimensionReadings(
            ScoreDimension.entries.mapIndexed { position, dimension ->
                DimensionReading(
                    dimension, values.getOrNull(position) ?: NEUTRAL_SCORE, opportunities.at(position), evidence.at(position), turnWeight,
                )
            },
        )

    companion object {
        /**
         * 근거가 부족한 항목에 주는 중립 점수이자 수축([ScoreShrinkage])의 사전 평균.
         * 채점 프롬프트가 말하는 평범한 대화 구간(50~65)의 가운데쯤이다. 이 값을 쓰는 곳은 모두 이 상수를 참조한다.
         */
        val NEUTRAL_SCORE = BigDecimal(55)
    }
}
