package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.participant.model.Region
import java.time.Instant

/**
 * 순위에 오를 참가자 한 명의 최신 평가, 참가자, 배송 지역.
 */
class RankingCandidate(
    private val assessment: Assessment,
    private val participant: Participant,
    private val region: Region,
) {
    fun scoreHundredths(): Int = assessment.scoreHundredths

    fun assessedAt(): Instant = assessment.createdAt

    fun participantId(): String = participant.id

    fun assessmentId(): String = assessment.id

    fun coordinate(): Coordinate = region.coordinate()

    fun hasSameScoreAs(other: RankingCandidate): Boolean = scoreHundredths() == other.scoreHundredths()
}
