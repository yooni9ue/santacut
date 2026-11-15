package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.UtcInstantConverter
import com.santacut.domain.ranking.model.Season
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 비동기 분석 작업. 상태는 대기 → 실행 → 성공·실패·근거 부족 순서로만 바뀐다.
 * 단, 너무 오래 기다린 대기 작업은 실행을 거치지 않고 만료(실패)로 끝난다.
 * 작업자는 리스 토큰을 쥔 동안만 결과를 기록할 수 있다.
 */
@Entity
@Table(name = "analysis_jobs")
class AnalysisJob(
    id: String,
    userId: String,
    idempotencyKey: String,
    requestHash: String,
    source: DataSource,
    payload: String?,
    createdAt: Instant,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String = userId
        protected set

    @Column(name = "idempotency_key", nullable = false)
    var idempotencyKey: String = idempotencyKey
        protected set

    @Column(name = "request_hash", nullable = false)
    var requestHash: String = requestHash
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    var source: DataSource = source
        protected set

    @Column(name = "payload")
    var payload: String? = payload
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AnalysisJobStatus = AnalysisJobStatus.QUEUED
        protected set

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0
        protected set

    @Column(name = "lease_token")
    var leaseToken: String? = null
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "lease_until")
    var leaseUntil: Instant? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code")
    var errorCode: AnalysisFailureCode? = null
        protected set

    @Column(name = "assessment_id")
    var assessmentId: String? = null
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = createdAt
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "finished_at")
    var finishedAt: Instant? = null
        protected set

    fun matchesRequest(payload: AnalysisPayload): Boolean = requestHash == payload.hash()

    fun hasSucceeded(): Boolean = status == AnalysisJobStatus.SUCCEEDED

    fun hasFailed(): Boolean = status == AnalysisJobStatus.FAILED || status == AnalysisJobStatus.INSUFFICIENT_DATA

    fun isHeldBy(lease: String): Boolean = leaseToken == lease && status == AnalysisJobStatus.RUNNING

    /** 대기 중이거나, 실행 중이지만 리스가 끝나 다른 작업자가 다시 잡을 수 있는지. */
    fun isClaimableAt(now: Instant): Boolean = status == AnalysisJobStatus.QUEUED || isLeaseExpiredAt(now)

    /** 작업자가 리스를 잡고 한 번 더 시도한다. 대기 중이거나 리스가 끝난 실행 작업만 잡을 수 있다. */
    fun startAttempt(lease: String, now: Instant) {
        verifyTransition(isClaimableAt(now))
        status = AnalysisJobStatus.RUNNING
        attempts++
        leaseToken = lease
        leaseUntil = now.plus(LEASE_DURATION)
    }

    /** 실행 중인 작업만 성공으로 끝낼 수 있다. */
    fun succeed(assessmentId: String, finishedAt: Instant) {
        verifyTransition(status == AnalysisJobStatus.RUNNING)
        status = AnalysisJobStatus.SUCCEEDED
        this.assessmentId = assessmentId
        finish(finishedAt)
    }

    /** 실행 중인 작업만 실패로 끝낼 수 있다. */
    fun fail(code: AnalysisFailureCode, finishedAt: Instant) {
        verifyTransition(status == AnalysisJobStatus.RUNNING)
        finishWith(code, finishedAt)
    }

    /** 너무 오래 기다린 대기 작업을 [AnalysisFailureCode.EXPIRED]로 끝낸다. 대기 중인 작업만 만료할 수 있다. */
    fun expire(finishedAt: Instant) {
        verifyTransition(status == AnalysisJobStatus.QUEUED)
        finishWith(AnalysisFailureCode.EXPIRED, finishedAt)
    }

    /**
     * 완료 시점에 시즌이 이미 마감됐으면 평가를 반영하지 않고 [AnalysisFailureCode.SEASON_FROZEN]으로 끝낸다.
     * @return 시즌 마감으로 실패 처리했으면 참
     */
    fun freezeIfSeasonClosed(season: Season, completedAt: Instant): Boolean {
        if (!season.isClosedAt(completedAt)) return false
        fail(AnalysisFailureCode.SEASON_FROZEN, completedAt)
        return true
    }

    fun requirePayload(): String = payload ?: throw SantaCutException(ErrorCode.ANALYSIS_PAYLOAD_MISSING)

    /**
     * 응답용 진행 단계. 성공한 작업은 참가자의 최신 순위 계산 시각([latestRankedAt])이
     * 완료 시각 이후일 때만 순위에 반영된 것으로 본다.
     */
    fun progress(latestRankedAt: Instant?): AnalysisProgress = status.toProgress(isRankedBy(latestRankedAt))

    private fun isRankedBy(latestRankedAt: Instant?): Boolean {
        val finished = finishedAt ?: return false
        return latestRankedAt != null && !latestRankedAt.isBefore(finished)
    }

    private fun isLeaseExpiredAt(now: Instant): Boolean =
        status == AnalysisJobStatus.RUNNING && leaseUntil?.isBefore(now) == true

    private fun verifyTransition(allowed: Boolean) {
        if (!allowed) throw SantaCutException(ErrorCode.ANALYSIS_JOB_STATE_INVALID, "status=$status")
    }

    private fun finishWith(code: AnalysisFailureCode, finishedAt: Instant) {
        status = code.resultingStatus
        errorCode = code
        finish(finishedAt)
    }

    /** 끝난 작업은 제출 원문을 더 들고 있지 않는다. */
    private fun finish(finishedAt: Instant) {
        payload = null
        this.finishedAt = finishedAt
    }

    companion object {
        /** 한 작업을 시도할 수 있는 최대 횟수. */
        const val MAX_ATTEMPTS = 3

        /** 한 참가자가 [SUBMISSION_WINDOW] 동안 제출할 수 있는 작업 수. */
        const val MAX_DAILY_SUBMISSIONS = 3

        /**
         * 참가자별 제출 한도를 세는 기간. 지금부터 거슬러 올라가는 이동 구간이다.
         * [QUEUE_EXPIRY]와 길이는 같지만 뜻이 다르다. 이 값은 참가자 한도, 저 값은 작업 하나의 대기 수명이다.
         */
        val SUBMISSION_WINDOW: Duration = Duration.ofDays(1)

        /**
         * 대기 상태로 이 시간이 지나면 만료한다. AI 예산이 바닥나 오래 밀린 작업을 정리하기 위한 값으로,
         * 제출 한도 기간([SUBMISSION_WINDOW])과는 따로 조정할 수 있다.
         */
        val QUEUE_EXPIRY: Duration = Duration.ofHours(24)

        private val LEASE_DURATION: Duration = Duration.ofMinutes(3)

        /** 이 시각보다 먼저 만든 대기 작업은 [QUEUE_EXPIRY]를 넘겨 만료 대상이다. */
        fun queueExpiryCutoff(now: Instant): Instant = now.minus(QUEUE_EXPIRY)

        /** 제출 한도를 셀 기간의 시작 시각. 이 시각 이후 만든 작업만 센다. */
        fun submissionWindowStart(now: Instant): Instant = now.minus(SUBMISSION_WINDOW)

        /** 기간 안에 이미 낸 작업 수가 한도에 닿았으면 더 받지 않는다. */
        fun verifyWithinDailyLimit(recentSubmissions: Long) {
            if (recentSubmissions >= MAX_DAILY_SUBMISSIONS) throw SantaCutException(ErrorCode.DAILY_USER_LIMIT)
        }

        fun enqueue(
            participantId: UUID,
            key: IdempotencyKey,
            submission: AnalysisSubmission,
            payload: AnalysisPayload,
            createdAt: Instant,
        ): AnalysisJob = AnalysisJob(
            UUID.randomUUID().toString(), participantId.toString(), key.value,
            payload.hash(), submission.source, payload.json, createdAt,
        )
    }
}
