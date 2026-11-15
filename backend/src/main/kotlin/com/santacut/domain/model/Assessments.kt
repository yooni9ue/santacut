package com.santacut.domain.model

/**
 * 참가자별 최신 평가 목록.
 */
class Assessments(private val values: List<Assessment>) {
    /** 참가자와 지역이 모두 남아 있는 평가만 골라 배송 순서로 줄 세운다. */
    fun toRankingEntries(participants: Participants, regions: Regions): RankingEntries =
        RankingEntries.sorted(values.mapNotNull { candidateOf(it, participants, regions) })

    private fun candidateOf(assessment: Assessment, participants: Participants, regions: Regions): RankingCandidate? {
        val participant = participants.find(assessment.userId) ?: return null
        val region = regions.find(participant.regionCode) ?: return null
        return RankingCandidate(assessment, participant, region)
    }
}
