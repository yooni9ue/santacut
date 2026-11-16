package com.santacut.domain.analysis.model

import com.santacut.domain.participant.model.Participants
import com.santacut.domain.participant.model.Regions
import com.santacut.domain.ranking.model.RankingCandidate
import com.santacut.domain.ranking.model.RankingEntries

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

    /**
     * 장난기 높낮이를 가르는 기준값. 장난기를 아는 평가가 [MIN_PARTICIPANTS_FOR_MEDIAN]개 이상이면 그 중앙값,
     * 그보다 적으면 [DEFAULT_PLAYFULNESS_SPLIT]이다. 참가자가 적을 때 유형이 한쪽으로 쏠리지 않게 하기 위해서다.
     */
    fun playfulnessSplit(): Int {
        val known = values.mapNotNull { assessment -> assessment.playfulness }.sorted()
        if (known.size < MIN_PARTICIPANTS_FOR_MEDIAN) return DEFAULT_PLAYFULNESS_SPLIT
        return known[known.size / 2]
    }

    /** 유형이 [archetype]인 평가의 비율(%, 반올림). 평가가 없으면 0이다. */
    fun archetypeSharePercent(archetype: Archetype, playfulnessSplit: Int): Int {
        if (values.isEmpty()) return 0
        val same = values.count { assessment -> assessment.archetype(playfulnessSplit) == archetype }
        return (same * PERCENT + values.size / 2) / values.size
    }

    companion object {
        const val MIN_PARTICIPANTS_FOR_MEDIAN = 30
        const val DEFAULT_PLAYFULNESS_SPLIT = 50
        private const val PERCENT = 100
    }
}
