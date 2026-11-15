package com.santacut.domain.ranking.model

import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 한 번의 순위 계산에서 정해진 참가자의 순위, 배송 순서, 도착 예정 시각.
 */
@Entity
@Table(name = "result_snapshots")
class ResultSnapshot(
    id: String,
    userId: String,
    assessmentId: String,
    rank: Int,
    deliverySequence: Int,
    eta: Instant,
    computedAt: Instant,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String = userId
        protected set

    @Column(name = "assessment_id", nullable = false)
    var assessmentId: String = assessmentId
        protected set

    @Column(name = "rank", nullable = false)
    var rank: Int = rank
        protected set

    @Column(name = "delivery_sequence", nullable = false)
    var deliverySequence: Int = deliverySequence
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "eta", nullable = false)
    var eta: Instant = eta
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "computed_at", nullable = false)
    var computedAt: Instant = computedAt
        protected set

    companion object {
        fun publish(
            candidate: RankingCandidate,
            rank: Int,
            deliverySequence: Int,
            eta: Instant,
            computedAt: Instant,
        ): ResultSnapshot = ResultSnapshot(
            UUID.randomUUID().toString(), candidate.participantId(), candidate.assessmentId(),
            rank, deliverySequence, eta, computedAt,
        )
    }
}
