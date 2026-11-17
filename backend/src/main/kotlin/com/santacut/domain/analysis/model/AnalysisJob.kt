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
 * 원본 파일로 받은 작업은 검증 → 대기 순서로 시작하고, 검증에 실패하면 대기를 거치지 않고 끝난다.
 * 너무 오래 기다린 검증·대기 작업은 실행을 거치지 않고 만료(실패)로 끝난다.
 * 작업자는 리스 토큰을 쥔 동안만 결과를 기록할 수 있다.
 * 같은 채점 입력으로 다시 낸 작업이 AI를 또 부르지 않도록, 결과가 입력에 따라 정해지는 작업에는 채점 입력 해시([digestHash])를 남긴다.
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
    status: AnalysisJobStatus = AnalysisJobStatus.QUEUED,
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
    var status: AnalysisJobStatus = status
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

    /** 모델 ID, 프롬프트 판, AI 사용자 메시지를 이은 값의 SHA-256. 성공·근거 부족·응답 오류로 끝난 작업에만 있다. */
    @Column(name = "digest_hash")
    var digestHash: String? = null
        protected set

    fun matchesRequestHash(hash: String): Boolean = requestHash == hash

    fun isValidating(): Boolean = status == AnalysisJobStatus.VALIDATING

    fun isHeldBy(lease: String): Boolean = leaseToken == lease && status == AnalysisJobStatus.RUNNING

    /** 대기 중이거나, 실행 중이지만 리스가 끝나 다른 작업자가 다시 잡을 수 있는지. */
    fun isClaimableAt(now: Instant): Boolean = status == AnalysisJobStatus.QUEUED || isLeaseExpiredAt(now)

    /** 작업자가 리스를 [leaseDuration] 동안 잡고 한 번 더 시도한다. 대기 중이거나 리스가 끝난 실행 작업만 잡을 수 있다. */
    fun startAttempt(lease: String, now: Instant, leaseDuration: Duration) {
        verifyTransition(isClaimableAt(now))
        status = AnalysisJobStatus.RUNNING
        attempts++
        leaseToken = lease
        leaseUntil = now.plus(leaseDuration)
    }

    /** 실행 중인 작업만 성공으로 끝낼 수 있다. [digestHash]가 있으면 같은 입력의 재제출에 이 결과를 다시 쓴다. */
    fun succeed(assessmentId: String, finishedAt: Instant, digestHash: String? = null) {
        verifyTransition(status == AnalysisJobStatus.RUNNING)
        status = AnalysisJobStatus.SUCCEEDED
        this.assessmentId = assessmentId
        this.digestHash = digestHash
        finish(finishedAt)
    }

    /** 실행 중인 작업만 실패로 끝낼 수 있다. [digestHash]가 있으면 같은 입력의 재제출도 AI 없이 같은 이유로 끝낸다. */
    fun fail(code: AnalysisFailureCode, finishedAt: Instant, digestHash: String? = null) {
        verifyTransition(status == AnalysisJobStatus.RUNNING)
        this.digestHash = digestHash
        finishWith(code, finishedAt)
    }

    /**
     * AI 제공자가 호출을 막았을 때 작업을 실패시키지 않고 대기열로 되돌린다. 이번 시도는 세지 않는다.
     * 오래 밀리면 대기 만료([QUEUE_EXPIRY])가 정리한다.
     */
    fun releaseLease() {
        verifyTransition(status == AnalysisJobStatus.RUNNING)
        attempts = maxOf(attempts - 1, 0)
        returnToQueue()
    }

    /**
     * 써 버린 시도를 센 채로 작업을 대기열로 되돌린다. 작업자가 죽어 리스가 끝난 작업과, 구간 몇 개를 이미 과금한 뒤
     * 제공자가 막은 작업이 해당한다. 대기열에서는 만든 순서대로 다시 잡히고, 오래 밀리면 대기 만료([QUEUE_EXPIRY])가 정리한다.
     * 시도를 [MAX_ATTEMPTS]번 다 썼으면 되돌리지 않고 [AnalysisFailureCode.RETRY_EXHAUSTED]로 끝낸다.
     */
    fun requeueSpentAttempt(now: Instant) {
        verifyTransition(status == AnalysisJobStatus.RUNNING)
        if (attempts >= MAX_ATTEMPTS) return finishWith(AnalysisFailureCode.RETRY_EXHAUSTED, now)
        returnToQueue()
    }

    private fun returnToQueue() {
        status = AnalysisJobStatus.QUEUED
        leaseToken = null
        leaseUntil = null
    }

    /** 같은 입력으로 성공한 작업이면 다시 쓸 평가 ID. */
    fun reusableAssessmentId(): String? = assessmentId.takeIf { status == AnalysisJobStatus.SUCCEEDED }

    /** 같은 입력으로 실패한 작업이면 다시 쓸 실패 이유. */
    fun reusableFailure(): AnalysisFailureCode? = errorCode.takeIf { status != AnalysisJobStatus.SUCCEEDED }

    /** 너무 오래 기다린 작업을 [AnalysisFailureCode.EXPIRED]로 끝낸다. 검증 중이거나 대기 중인 작업만 만료할 수 있다. */
    fun expire(finishedAt: Instant) {
        verifyTransition(isValidating() || status == AnalysisJobStatus.QUEUED)
        finishWith(AnalysisFailureCode.EXPIRED, finishedAt)
    }

    /**
     * 검증 작업자가 원본 파일 검증을 한 번 더 시도한다. 검증 중인 작업만 시도할 수 있다.
     * 검증 도중 작업자가 죽어(메모리 부족 등) [MAX_ATTEMPTS]번을 넘기면 다시 시도하지 않고
     * [AnalysisFailureCode.RETRY_EXHAUSTED]로 끝낸다. 같은 파일로 재시작을 되풀이하지 않기 위해서다.
     */
    fun startValidation(now: Instant) {
        verifyTransition(isValidating())
        attempts++
        if (attempts > MAX_ATTEMPTS) finishWith(AnalysisFailureCode.RETRY_EXHAUSTED, now)
    }

    /** 검증을 통과한 작업에 정규화한 제출 내용을 담아 대기열에 넣는다. 점수 산정 시도 횟수는 처음부터 센다. */
    fun queueValidated(payload: AnalysisPayload) {
        verifyTransition(isValidating())
        status = AnalysisJobStatus.QUEUED
        attempts = 0
        this.payload = payload.json
    }

    /** 원본 파일 검증에 실패한 작업을 끝낸다. 검증 중인 작업만 끝낼 수 있다. */
    fun rejectUpload(code: AnalysisFailureCode, finishedAt: Instant) {
        verifyTransition(isValidating())
        finishWith(code, finishedAt)
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
         * 대기 상태로 이 시간이 지나면 만료한다. AI가 꺼져 있거나 AI 비용 한도에 막혀 오래 밀린 작업을 정리하기 위한 값으로,
         * 제출 한도 기간([SUBMISSION_WINDOW])과는 따로 조정할 수 있다.
         */
        val QUEUE_EXPIRY: Duration = Duration.ofHours(24)

        /** 구간 호출이 모두 시간 상한까지 걸린 뒤 결과를 기록할 여유. */
        private val LEASE_MARGIN: Duration = Duration.ofMinutes(1)

        /**
         * AI 호출 하나의 시간 상한이 [callTimeout]일 때의 작업 리스 길이. 구간 수 상한([ScoringChunks.MAX_CHUNKS])만큼 호출이 모두
         * 상한까지 걸려도 리스가 남아 있어야 다른 작업자가 같은 작업을 다시 잡지 않는다.
         */
        fun leaseDurationFor(callTimeout: Duration): Duration =
            callTimeout.multipliedBy(ScoringChunks.MAX_CHUNKS.toLong()).plus(LEASE_MARGIN)

        /** 이 시각보다 먼저 만든 대기 작업은 [QUEUE_EXPIRY]를 넘겨 만료 대상이다. */
        fun queueExpiryCutoff(now: Instant): Instant = now.minus(QUEUE_EXPIRY)

        /** 제출 한도를 셀 기간의 시작 시각. 이 시각 이후 만든 작업만 센다. */
        fun submissionWindowStart(now: Instant): Instant = now.minus(SUBMISSION_WINDOW)

        /**
         * 기간 안에 이미 낸 작업 수가 한도에 닿았으면 더 받지 않는다.
         * 기간 안 가장 오래된 제출([oldestSubmittedAt])이 기간 밖으로 빠지는 시각을 다시 낼 수 있는 시각으로 알린다.
         */
        fun verifyWithinDailyLimit(recentSubmissions: Long, oldestSubmittedAt: Instant?) {
            if (recentSubmissions < MAX_DAILY_SUBMISSIONS) return
            throw SantaCutException(ErrorCode.DAILY_USER_LIMIT, retryAt = oldestSubmittedAt?.plus(SUBMISSION_WINDOW))
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

        /**
         * 원본 파일로 받은 작업. 검증 작업자가 파일을 정규화하기 전까지 페이로드가 없다.
         * [requestHash]는 파일 해시이며 같은 키로 다시 보낸 요청이 같은 파일인지 가린다.
         */
        fun receive(
            participantId: UUID,
            key: IdempotencyKey,
            source: DataSource,
            requestHash: String,
            createdAt: Instant,
        ): AnalysisJob = AnalysisJob(
            UUID.randomUUID().toString(), participantId.toString(), key.value,
            requestHash, source, null, createdAt, AnalysisJobStatus.VALIDATING,
        )
    }
}
