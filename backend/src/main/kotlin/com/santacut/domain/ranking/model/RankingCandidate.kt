package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.participant.model.Region
import java.math.BigDecimal
import java.time.Instant

/**
 * 순위에 오를 참가자 한 명의 최신 평가, 참가자, 배송 지역, 시즌 첫 심사 성공 시각.
 *
 * @property firstAssessedAt 이 참가자가 이번 시즌 처음 심사에 성공한 시각. 재심사해도 바뀌지 않는다.
 */
class RankingCandidate(
    private val assessment: Assessment,
    private val participant: Participant,
    private val region: Region,
    private val firstAssessedAt: Instant,
) {
    private val dimensionScores: ScoreComponents by lazy { assessment.dimensionScores() }

    fun scoreHundredths(): Int = assessment.scoreHundredths

    /** 동점 처리에 쓰는 항목 점수. */
    fun dimensionScore(dimension: ScoreDimension): BigDecimal = dimensionScores.scoreOf(dimension)

    fun firstAssessedAt(): Instant = firstAssessedAt

    fun participantId(): String = participant.id

    fun assessmentId(): String = assessment.id

    fun coordinate(): Coordinate = region.coordinate()
}
