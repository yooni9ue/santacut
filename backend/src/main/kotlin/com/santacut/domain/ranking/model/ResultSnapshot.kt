package com.santacut.domain.ranking.model

import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

/**
 * 한 번의 순위 계산에서 정해진 참가자의 순위, 배송 순서, 도착 예정 시각.
 *
 * 스냅숏은 만든 뒤 바꾸지 않는다. ID를 직접 정하므로 [Persistable]로 새 행임을 알려,
 * 저장할 때 행마다 SELECT로 있는지 확인(merge)하지 않고 바로 INSERT(persist)하게 한다. 그래야 묶음 INSERT가 된다.
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
    tied: Boolean = false,
) : Persistable<String> {
    @Id
    @Column(name = "id")
    private var id: String = id

    /** DB에서 읽었거나 이미 저장한 행인지. 저장하지 않는 값이다. */
    @Transient
    private var persisted: Boolean = false

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

    /** 같은 점수의 다른 참가자가 있어 공동 순위인지. 이 컬럼이 없던 기존 DB에도 붙도록 기본값을 둔다. */
    @Column(name = "tied", nullable = false, columnDefinition = TIED_COLUMN_DEFINITION)
    var tied: Boolean = tied
        protected set

    override fun getId(): String = id

    override fun isNew(): Boolean = !persisted

    @PostLoad
    @PostPersist
    private fun markPersisted() {
        persisted = true
    }

    companion object {
        private const val TIED_COLUMN_DEFINITION = "INTEGER NOT NULL DEFAULT 0"

        fun publish(
            candidate: RankingCandidate,
            rank: Int,
            tied: Boolean,
            deliverySequence: Int,
            eta: Instant,
            computedAt: Instant,
        ): ResultSnapshot = ResultSnapshot(
            UUID.randomUUID().toString(), candidate.participantId(), candidate.assessmentId(),
            rank, deliverySequence, eta, computedAt, tied,
        )
    }
}
