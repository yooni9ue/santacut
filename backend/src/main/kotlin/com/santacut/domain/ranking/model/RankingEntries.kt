package com.santacut.domain.ranking.model

import com.santacut.domain.global.model.Coordinate
import java.time.Instant
import java.time.ZonedDateTime

/**
 * 배송 순서로 줄 세운 순위 후보.
 * 점수가 높은 순, 같으면 먼저 평가받은 순, 그다음 참가자 ID 순으로 배송한다.
 */
class RankingEntries private constructor(private val values: List<RankingCandidate>) {
    /** 공동 순위를 허용하는 경쟁 순위(1, 2, 2, 4 …). */
    fun competitionRanks(): List<Int> =
        values.indices.scan(0) { rank, index -> if (sharesRankWithPrevious(index)) rank else index + 1 }.drop(1)

    /** 각 후보가 앞이나 뒤 후보와 점수가 같아 공동 순위인지. */
    fun tiedFlags(): List<Boolean> =
        values.indices.map { index -> sharesRankWithPrevious(index) || sharesRankWithPrevious(index + 1) }

    /** 출발지에서 각 참가자까지 누적한 작업 시간(초). */
    fun cumulativeWorkSeconds(origin: Coordinate): List<Double> =
        (listOf(origin) + values.map { it.coordinate() })
            .zipWithNext { from, to -> Delivery.legSeconds(from, to) }
            .runningReduce(Double::plus)

    /** 순위, 공동 순위 여부, 배송 순서, 도착 예정 시각을 담은 새 스냅숏을 만든다. */
    fun toSnapshots(origin: Coordinate, deliveryStart: ZonedDateTime, computedAt: Instant): ResultSnapshots {
        val ranks = competitionRanks()
        val tied = tiedFlags()
        val workSeconds = cumulativeWorkSeconds(origin)
        val snapshots = values.mapIndexed { index, candidate ->
            val eta = Delivery.arrival(deliveryStart, workSeconds[index]).toInstant()
            ResultSnapshot.publish(candidate, ranks[index], tied[index], index + 1, eta, computedAt)
        }
        return ResultSnapshots(snapshots)
    }

    private fun sharesRankWithPrevious(index: Int): Boolean =
        index in 1 until values.size && values[index - 1].hasSameScoreAs(values[index])

    companion object {
        private val DELIVERY_ORDER = compareByDescending<RankingCandidate> { it.scoreHundredths() }
            .thenBy { it.assessedAt() }
            .thenBy { it.participantId() }

        fun sorted(candidates: List<RankingCandidate>): RankingEntries =
            RankingEntries(candidates.sortedWith(DELIVERY_ORDER))
    }
}
