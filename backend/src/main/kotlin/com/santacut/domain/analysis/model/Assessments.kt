package com.santacut.domain.analysis.model

import com.santacut.domain.participant.model.Participants
import com.santacut.domain.participant.model.Regions
import com.santacut.domain.ranking.model.FirstAssessments
import com.santacut.domain.ranking.model.RankingCandidate
import com.santacut.domain.ranking.model.RankingEntries

/**
 * 참가자별 최신 평가 목록.
 */
class Assessments(private val values: List<Assessment>) {
    /**
     * 참가자와 지역이 모두 남아 있는 평가만 골라 배송 순서로 줄 세운다.
     * 첫 심사 성공 시각([firstAssessments])을 모르는 참가자는 이 평가 시각을 쓴다.
     */
    fun toRankingEntries(
        participants: Participants,
        regions: Regions,
        firstAssessments: FirstAssessments = FirstAssessments.EMPTY,
    ): RankingEntries = RankingEntries.sorted(values.mapNotNull { candidateOf(it, participants, regions, firstAssessments) })

    private fun candidateOf(
        assessment: Assessment,
        participants: Participants,
        regions: Regions,
        firstAssessments: FirstAssessments,
    ): RankingCandidate? {
        val participant = participants.find(assessment.userId) ?: return null
        val region = regions.find(participant.regionCode) ?: return null
        val firstAssessedAt = firstAssessments.of(assessment.userId) ?: assessment.createdAt
        return RankingCandidate(assessment, participant, region, firstAssessedAt)
    }

    /**
     * 장난기 높낮이를 가르는 기준값. 장난기를 아는 평가가 [MIN_PARTICIPANTS_FOR_MEDIAN]개 이상이면 그 중앙값,
     * 그보다 적으면 [DEFAULT_PLAYFULNESS_SPLIT]이다. 참가자가 적을 때 유형이 한쪽으로 쏠리지 않게 하기 위해서다.
     */
    fun playfulnessSplit(): Int {
        val known = values.mapNotNull { assessment -> assessment.playfulness }.sorted()
        if (known.size < MIN_PARTICIPANTS_FOR_MEDIAN) return DEFAULT_PLAYFULNESS_SPLIT
        return known[known.size / 2]
    }

    /** 장난기 기준값 [playfulnessSplit]으로 고른 유형별 평가 수. */
    fun archetypeCounts(playfulnessSplit: Int): ArchetypeCounts =
        ArchetypeCounts(values.groupingBy { assessment -> assessment.archetype(playfulnessSplit) }.eachCount())

    companion object {
        const val MIN_PARTICIPANTS_FOR_MEDIAN = 30
        const val DEFAULT_PLAYFULNESS_SPLIT = 50
    }
}
