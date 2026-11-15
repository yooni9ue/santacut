package com.santacut.domain.share.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.global.model.RandomToken
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.global.model.UtcInstantConverter
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.ranking.model.RankingStage
import com.santacut.domain.ranking.model.ResultSnapshot
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 공개 공유 링크. 만든 순간의 결과를 그대로 복사해 두어 이후 순위가 바뀌어도 내용이 변하지 않는다.
 * 취소하면 행을 지운다(소프트 삭제를 쓰지 않는다).
 */
@Entity
@Table(name = "shares")
class Share(
    token: String,
    userId: String,
    snapshotId: String,
    nickname: String,
    scoreHundredths: Int,
    rank: Int,
    eta: Instant,
    verdict: String,
    comment: String,
    computedAt: Instant,
    tied: Boolean = false,
    stage: RankingStage = RankingStage.PREVIEW,
    createdAt: Instant? = null,
) {
    @Id
    @Column(name = "token")
    var token: String = token
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String = userId
        protected set

    @Column(name = "snapshot_id", nullable = false)
    var snapshotId: String = snapshotId
        protected set

    @Column(name = "nickname", nullable = false)
    var nickname: String = nickname
        protected set

    @Column(name = "score_hundredths", nullable = false)
    var scoreHundredths: Int = scoreHundredths
        protected set

    @Column(name = "rank", nullable = false)
    var rank: Int = rank
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "eta", nullable = false)
    var eta: Instant = eta
        protected set

    @Column(name = "verdict", nullable = false)
    var verdict: String = verdict
        protected set

    @Column(name = "comment", nullable = false)
    var comment: String = comment
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "computed_at", nullable = false)
    var computedAt: Instant = computedAt
        protected set

    /** 공유 당시 공동 순위였는지. 이 컬럼이 없던 기존 DB에도 붙도록 기본값을 둔다. */
    @Column(name = "tied", nullable = false, columnDefinition = TIED_COLUMN_DEFINITION)
    var tied: Boolean = tied
        protected set

    /** 공유 당시 순위 상태. 이 컬럼이 없던 기존 DB에도 붙도록 기본값을 둔다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, columnDefinition = STAGE_COLUMN_DEFINITION)
    var stage: RankingStage = stage
        protected set

    /**
     * 공유 링크를 만든 시각. SQLite는 시각 함수 기본값으로 컬럼을 더할 수 없어 기존 DB의 행은 비어 있다.
     * 비어 있으면 [sharedAt]이 계산 시각으로 대신한다.
     */
    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", columnDefinition = CREATED_AT_COLUMN_DEFINITION)
    var createdAt: Instant? = createdAt
        protected set

    /** 소수점 둘째 자리까지 표시한 점수(예: `82.45`). */
    fun displayScore(): String =
        BigDecimal(scoreHundredths).movePointLeft(SCORE_DECIMAL_PLACES).setScale(SCORE_DECIMAL_PLACES).toPlainString()

    /** 서울 시각으로 표시한 도착 예정일. */
    fun displayEta(): String = formatSeoul(eta)

    /** 공동 순위면 `공동 3위`, 아니면 `3위`. */
    fun displayRank(): String = if (tied) "$TIED_PREFIX$rank$RANK_SUFFIX" else "$rank$RANK_SUFFIX"

    /** 서울 시각으로 표시한 순위 계산 시각. */
    fun displayComputedAt(): String = formatSeoul(computedAt)

    /** 공유 당시 순위 상태 이름. */
    fun displayStage(): String = stage.label

    /** 서울 시각으로 표시한 공유 시각. */
    fun displaySharedAt(): String = formatSeoul(sharedAt())

    /** 공유 링크를 만든 시각. 기존 DB의 행처럼 비어 있으면 계산 시각을 쓴다. */
    fun sharedAt(): Instant = createdAt ?: computedAt

    private fun formatSeoul(instant: Instant): String = instant.atZone(SeoulTime.ZONE).format(SEOUL_FORMAT)

    companion object {
        private const val SCORE_DECIMAL_PLACES = 2
        private const val TIED_PREFIX = "공동 "
        private const val RANK_SUFFIX = "위"
        private const val TIED_COLUMN_DEFINITION = "INTEGER NOT NULL DEFAULT 0"
        private const val STAGE_COLUMN_DEFINITION = "TEXT NOT NULL DEFAULT 'PREVIEW'"
        private const val CREATED_AT_COLUMN_DEFINITION = "TEXT"
        private val SEOUL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")

        /** 공유 링크를 만든다. 순위, 공동 순위 여부, 상태, 시각은 만든 순간 값으로 고정한다. */
        fun issue(
            participant: Participant,
            snapshot: ResultSnapshot,
            assessment: Assessment,
            stage: RankingStage,
            issuedAt: Instant,
        ): Share = Share(
            RandomToken.generate(), participant.id, snapshot.id, participant.nickname, assessment.scoreHundredths,
            snapshot.rank, snapshot.eta, assessment.verdict, assessment.comment, snapshot.computedAt,
            snapshot.tied, stage, issuedAt,
        )
    }
}
