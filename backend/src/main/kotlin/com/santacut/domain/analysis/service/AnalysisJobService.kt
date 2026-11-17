package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisJobChange
import com.santacut.domain.analysis.model.AnalysisJobStatus
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisStreamLimit
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.global.config.AnalysisStreamProperties
import com.santacut.domain.global.config.AppProperties
import com.santacut.domain.global.config.ClientRateLimitProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.ClientRateLimit
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * 분석 작업을 저장하고 찾는다. 작업 행을 저장할 때마다 [AnalysisJobChange]를 알려 진행 상황 스트림이 커밋 뒤 새 상태를 보내게 한다.
 */
@Service
class AnalysisJobService(
    private val analysisJobRepository: AnalysisJobRepository,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties,
    private val eventPublisher: ApplicationEventPublisher,
    analysisStreamProperties: AnalysisStreamProperties,
    scoreModelProperties: ScoreModelProperties,
    clientRateLimitProperties: ClientRateLimitProperties,
) {
    private val submissionLimit = ClientRateLimit(
        clientRateLimitProperties.submissionsPerClient, clientRateLimitProperties.submissionWindow,
    )
    private val streamLimit = AnalysisStreamLimit(analysisStreamProperties.maxOpenStreams, analysisStreamProperties.retryAfter)
    private val leaseDuration = AnalysisJob.leaseDurationFor(scoreModelProperties.callTimeout)

    fun toPayload(submission: AnalysisSubmission): AnalysisPayload =
        AnalysisPayload(objectMapper.writeValueAsString(submission))

    /** 시즌 마감 전이고 이벤트 범위가 맞는지 확인한다. */
    fun verifyAcceptable(submission: AnalysisSubmission, now: Instant) {
        season().verifyOpenAt(now)
        submission.verifyAcceptableAt(now)
    }

    /** 완료 시점에 시즌이 마감됐으면 작업을 시즌 마감 실패로 저장하고 참을 돌려준다. */
    fun freezeIfSeasonClosed(job: AnalysisJob, completedAt: Instant): Boolean {
        val frozen = job.freezeIfSeasonClosed(season(), completedAt)
        if (frozen) save(job)
        return frozen
    }

    /** 같은 키로 이미 받은 작업. 본문이 다르면 충돌이다. */
    fun findReplay(participantId: UUID, key: IdempotencyKey, payload: AnalysisPayload): AnalysisJob? =
        findReplayByHash(participantId, key, payload.hash())

    /** 같은 키로 이미 받은 작업. 요청 해시가 다르면 충돌이다. JSON 제출과 원본 파일 작업이 키를 함께 쓴다. */
    fun findReplayByHash(participantId: UUID, key: IdempotencyKey, requestHash: String): AnalysisJob? =
        findByKey(participantId, key)?.let { verifyReplay(it, requestHash) }

    /** 같은 키로 이미 받은 작업. 원본 파일은 내용을 읽기 전에 이것으로 다시 보낸 요청인지부터 본다. */
    fun findByKey(participantId: UUID, key: IdempotencyKey): AnalysisJob? =
        analysisJobRepository.findByUserIdAndIdempotencyKey(participantId.toString(), key.value)

    /** 같은 키로 받은 [existing]과 요청 해시가 같으면 그 작업이고, 다르면 [ErrorCode.IDEMPOTENCY_CONFLICT]다. */
    fun verifyReplay(existing: AnalysisJob, requestHash: String): AnalysisJob {
        if (!existing.matchesRequestHash(requestHash)) throw SantaCutException(ErrorCode.IDEMPOTENCY_CONFLICT)
        return existing
    }

    /**
     * 새 작업을 받을 수 있는지 확인한다. 참가자별 하루 제출 한도(JSON 제출과 원본 파일 합산)와
     * 클라이언트 IP([clientIp])별 제출 한도를 함께 본다. IP 한도는 이 확인을 부를 때마다 하나씩 센다.
     */
    fun verifyAcceptingSubmission(participantId: UUID, clientIp: String?, now: Instant) {
        verifyWithinDailyLimit(participantId, now)
        submissionLimit.acquire(clientIp, now)
    }

    fun enqueue(
        participantId: UUID,
        key: IdempotencyKey,
        submission: AnalysisSubmission,
        payload: AnalysisPayload,
        now: Instant,
    ): AnalysisJob = save(AnalysisJob.enqueue(participantId, key, submission, payload, now))

    /** 원본 파일로 받은 작업을 검증 상태로 저장한다. 한도는 먼저 [verifyAcceptingSubmission]으로 확인한다. */
    fun receiveUpload(
        participantId: UUID,
        key: IdempotencyKey,
        source: DataSource,
        requestHash: String,
        now: Instant,
    ): AnalysisJob = save(AnalysisJob.receive(participantId, key, source, requestHash, now))

    /** 진행 상황 스트림을 하나 더 열 수 있는지 확인한다. [replacing]이면 이 참가자의 이전 스트림을 대신한다. */
    fun verifyStreamOpenable(openStreams: Int, replacing: Boolean, now: Instant) =
        streamLimit.verifyOpenable(openStreams, replacing, now)

    fun find(jobId: String): AnalysisJob? = analysisJobRepository.findById(jobId).orElse(null)

    fun getOwned(jobId: UUID, participantId: UUID): AnalysisJob =
        analysisJobRepository.findByIdAndUserId(jobId.toString(), participantId.toString())
            ?: throw SantaCutException(ErrorCode.NOT_FOUND)

    /** 참가자가 가장 최근에 낸 작업. 낸 작업이 없으면 [ErrorCode.NOT_FOUND]다. */
    fun getLatestOwned(participantId: UUID): AnalysisJob =
        analysisJobRepository.findFirstByUserIdOrderByCreatedAtDesc(participantId.toString())
            ?: throw SantaCutException(ErrorCode.NOT_FOUND)

    /** 너무 오래 기다린 검증·대기 작업과 재시도를 다 쓴 실행 작업을 하나씩 실패로 정리한다. */
    fun expireOverdueJobs(now: Instant) {
        analysisJobRepository.findFirstByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
            AnalysisJobStatus.EXPIRABLE, AnalysisJob.queueExpiryCutoff(now),
        )?.let { expire(it, now) }
        analysisJobRepository.findFirstByStatusAndLeaseUntilBeforeAndAttemptsGreaterThanEqualOrderByCreatedAtAsc(
            AnalysisJobStatus.RUNNING, now, AnalysisJob.MAX_ATTEMPTS,
        )?.let { fail(it, AnalysisFailureCode.RETRY_EXHAUSTED, now) }
    }

    /** 가장 오래된 대기 작업, 없으면 리스가 끝나 다시 시도할 수 있는 실행 작업. */
    fun findClaimable(now: Instant): AnalysisJob? =
        analysisJobRepository.findFirstByStatusOrderByCreatedAtAsc(AnalysisJobStatus.QUEUED)
            ?: analysisJobRepository.findFirstByStatusAndLeaseUntilBeforeAndAttemptsLessThanOrderByCreatedAtAsc(
                AnalysisJobStatus.RUNNING, now, AnalysisJob.MAX_ATTEMPTS,
            )

    fun startAttempt(job: AnalysisJob, lease: String, now: Instant): AnalysisJob {
        job.startAttempt(lease, now, leaseDuration)
        return save(job)
    }

    /** 아직 이 리스를 쥐고 실행 중인 작업. 다른 작업자가 가져갔으면 null이다. */
    fun findHeld(jobId: String, lease: String): AnalysisJob? =
        analysisJobRepository.findById(jobId).orElse(null)?.takeIf { it.isHeldBy(lease) }

    fun succeed(job: AnalysisJob, assessmentId: String, finishedAt: Instant, digestHash: String? = null) {
        job.succeed(assessmentId, finishedAt, digestHash)
        save(job)
    }

    fun fail(job: AnalysisJob, code: AnalysisFailureCode, finishedAt: Instant, digestHash: String? = null) {
        job.fail(code, finishedAt, digestHash)
        save(job)
    }

    /** AI 제공자가 호출을 막아 작업을 대기열로 되돌린다. */
    fun releaseLease(job: AnalysisJob) {
        job.releaseLease()
        save(job)
    }

    /** 같은 참가자가 같은 채점 입력([digestHash])으로 낸 작업 가운데 가장 최근에 끝난 것. */
    fun findSameDigest(job: AnalysisJob, digestHash: String): AnalysisJob? =
        analysisJobRepository.findFirstByUserIdAndDigestHashAndFinishedAtIsNotNullOrderByFinishedAtDesc(job.userId, digestHash)

    /**
     * 검증 중인 작업의 검증을 한 번 더 시작한다. 작업이 없거나 이미 끝났으면 null이다.
     * 시도를 다 써서 [AnalysisFailureCode.RETRY_EXHAUSTED]로 끝났어도 저장하고 null을 돌려준다.
     */
    fun startValidation(jobId: String, now: Instant): AnalysisJob? {
        val job = findValidating(jobId) ?: return null
        job.startValidation(now)
        return save(job).takeIf { it.isValidating() }
    }

    /** 검증을 통과한 작업에 정규화한 제출 내용을 담아 대기열에 넣는다. 그사이 끝난 작업은 건드리지 않는다. */
    fun queueValidated(jobId: String, submission: AnalysisSubmission) {
        val job = findValidating(jobId) ?: return
        job.queueValidated(toPayload(submission))
        save(job)
    }

    /** 검증에 실패한 작업을 끝낸다. 그사이 끝난 작업은 건드리지 않는다. */
    fun rejectUpload(jobId: String, code: AnalysisFailureCode, finishedAt: Instant) {
        val job = findValidating(jobId) ?: return
        job.rejectUpload(code, finishedAt)
        save(job)
    }

    private fun expire(job: AnalysisJob, now: Instant) {
        job.expire(now)
        save(job)
    }

    private fun findValidating(jobId: String): AnalysisJob? =
        analysisJobRepository.findById(jobId).orElse(null)?.takeIf { it.isValidating() }

    /** 기간 안에 낸 작업 수가 한도에 닿았으면 더 받지 않는다. JSON 제출과 원본 파일 제출을 함께 센다. */
    private fun verifyWithinDailyLimit(participantId: UUID, now: Instant) {
        val userId = participantId.toString()
        val windowStart = AnalysisJob.submissionWindowStart(now)
        AnalysisJob.verifyWithinDailyLimit(
            analysisJobRepository.countByUserIdAndCreatedAtAfter(userId, windowStart),
            analysisJobRepository.findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, windowStart)?.createdAt,
        )
    }

    private fun season(): Season = Season(appProperties.seasonYear)

    /** 작업 행을 저장하고 저장했다는 사실을 알린다. 받는 쪽은 트랜잭션이 커밋된 뒤에 처리한다. */
    private fun save(job: AnalysisJob): AnalysisJob =
        analysisJobRepository.save(job).also { eventPublisher.publishEvent(AnalysisJobChange(it.id)) }
}
