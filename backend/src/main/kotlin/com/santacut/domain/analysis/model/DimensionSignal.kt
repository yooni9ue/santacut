package com.santacut.domain.analysis.model

/**
 * 채점 항목 하나의 낱말 신호. AI에는 이 값만 통계로 보낸다.
 *
 * @property hits 부정·반어 없이 항목 낱말이 든 본인 메시지 수
 * @property negated 항목 낱말에 부정·반어가 붙은 본인 메시지 수([ScoreTheme.isNegatedIn])
 * @property hitWeeks [hits]에 든 메시지가 있는 주 수
 * @property activeWeeks 본인 메시지가 하나라도 있는 주 수
 * @property shown 표본 줄 가운데 항목 낱말이 든 줄 수
 */
data class DimensionSignal(
    val dimension: ScoreDimension,
    val hits: Int,
    val negated: Int,
    val hitWeeks: Int,
    val activeWeeks: Int,
    val shown: Int,
)
