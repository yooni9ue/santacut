package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.global.model.Coordinate
import java.time.Instant
import java.time.ZonedDateTime

/**
 * 배송 순서로 줄 세운 순위 후보.
 * 점수가 높은 순, 같으면 배려·공감, 존중하는 소통, 긍정·응원, 나눔·협력 점수가 높은 순(backend-design.md §5)으로 순위를 정한다.
 * 모든 점수가 같은 공동 순위 안에서는 시즌 첫 심사 성공 시각이 이른 순, 그다음 참가자 ID 순으로 배송한다.
 */
class RankingEntries private constructor(private val values: List<RankingCandidate>) {
    /** 공동 순위를 허용하는 경쟁 순위(1, 2, 2, 4 …). */
    fun competitionRanks(): List<Int> =
        values.indices.scan(0) { rank, index -> if (sharesRankWithPrevious(index)) rank else index + 1 }.drop(1)

    /** 각 후보가 앞이나 뒤 후보와 점수가 모두 같아 공동 순위인지. */
    fun tiedFlags(): List<Boolean> =
        values.indices.map { index -> sharesRankWithPrevious(index) || sharesRankWithPrevious(index + 1) }

    /** 출발지에서 각 참가자까지 누적한 작업 시간(초). */
    fun cumulativeWorkSeconds(origin: Coordinate): List<Double> =
        (listOf(origin) + values.map { it.coordinate() })
            .zipWithNext { from, to -> Delivery.legSeconds(from, to) }
            .runningReduce(Double::plus)

    /**
     * 순위, 공동 순위 여부, 배송 순서, 도착 예정 시각을 담은 새 스냅숏을 만든다.
     * 도착 예정 시각은 [activeDays]일의 배송 기간 안에 들어오도록 맞춘다([DeliverySchedule]).
     */
    fun toSnapshots(origin: Coordinate, deliveryStart: ZonedDateTime, computedAt: Instant, activeDays: Int): ResultSnapshots {
        val ranks = competitionRanks()
        val tied = tiedFlags()
        val workSeconds = cumulativeWorkSeconds(origin)
        val schedule = DeliverySchedule.fit(deliveryStart, workSeconds.lastOrNull() ?: 0.0, activeDays)
        val snapshots = values.mapIndexed { index, candidate ->
            val eta = schedule.arrival(workSeconds[index]).toInstant()
            ResultSnapshot.publish(candidate, ranks[index], tied[index], index + 1, eta, computedAt)
        }
        return ResultSnapshots(snapshots)
    }

    private fun sharesRankWithPrevious(index: Int): Boolean =
        index in 1 until values.size && SCORE_ORDER.compare(values[index - 1], values[index]) == 0

    companion object {
        /** 공개 순위를 정하는 점수 순서. 이 순서로 같으면 공동 순위다. */
        private val SCORE_ORDER = compareByDescending<RankingCandidate> { it.scoreHundredths() }
            .thenByDescending { it.dimensionScore(ScoreDimension.CARE_EMPATHY) }
            .thenByDescending { it.dimensionScore(ScoreDimension.RESPECTFUL_COMMUNICATION) }
            .thenByDescending { it.dimensionScore(ScoreDimension.POSITIVITY_SUPPORT) }
            .thenByDescending { it.dimensionScore(ScoreDimension.COOPERATION_SHARING) }

        private val DELIVERY_ORDER = SCORE_ORDER
            .thenBy { it.firstAssessedAt() }
            .thenBy { it.participantId() }

        fun sorted(candidates: List<RankingCandidate>): RankingEntries =
            RankingEntries(candidates.sortedWith(DELIVERY_ORDER))
    }
}
