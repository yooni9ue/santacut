package com.santacut.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
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

    /** 소수점 둘째 자리까지 표시한 점수(예: `82.45`). */
    fun displayScore(): String =
        BigDecimal(scoreHundredths).movePointLeft(SCORE_DECIMAL_PLACES).setScale(SCORE_DECIMAL_PLACES).toPlainString()

    /** 서울 시각으로 표시한 도착 예정일. */
    fun displayEta(): String = eta.atZone(SeoulTime.ZONE).format(ETA_FORMAT)

    /** 데이터베이스 형식 그대로의 계산 시각(UTC). */
    fun displayComputedAt(): String = UtcDatabaseTime.format(computedAt)

    companion object {
        private const val SCORE_DECIMAL_PLACES = 2
        private val ETA_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")

        fun issue(participant: Participant, snapshot: ResultSnapshot, assessment: Assessment): Share = Share(
            RandomToken.generate(), participant.id, snapshot.id, participant.nickname, assessment.scoreHundredths,
            snapshot.rank, snapshot.eta, assessment.verdict, assessment.comment, snapshot.computedAt,
        )
    }
}
