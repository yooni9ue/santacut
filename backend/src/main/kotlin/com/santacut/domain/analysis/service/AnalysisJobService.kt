package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisJobStatus
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.global.config.AppProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class AnalysisJobService(
    private val analysisJobRepository: AnalysisJobRepository,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties,
) {
    fun toPayload(submission: AnalysisSubmission): AnalysisPayload =
        AnalysisPayload(objectMapper.writeValueAsString(submission))

    /** 시즌 마감 전이고 이벤트 범위가 맞는지 확인한다. */
    fun verifyAcceptable(submission: AnalysisSubmission, now: Instant) {
        season().verifyOpenAt(now)
        submission.verifyAcceptableAt(now)
    }

    fun verifySeasonOpen(now: Instant) {
        season().verifyOpenAt(now)
    }

    /** 완료 시점에 시즌이 마감됐으면 작업을 시즌 마감 실패로 저장하고 참을 돌려준다. */
    fun freezeIfSeasonClosed(job: AnalysisJob, completedAt: Instant): Boolean {
        val frozen = job.freezeIfSeasonClosed(season(), completedAt)
        if (frozen) analysisJobRepository.save(job)
        return frozen
    }

    /** 같은 키로 이미 받은 작업. 본문이 다르면 충돌이다. */
    fun findReplay(participantId: UUID, key: IdempotencyKey, payload: AnalysisPayload): AnalysisJob? =
        findReplayByHash(participantId, key, payload.hash())

    /** 같은 키로 이미 받은 원본 파일 작업. 파일·출처·본인 이름 해시가 다르면 충돌이다. JSON 제출 작업과도 키를 함께 쓴다. */
    fun findReplayByHash(participantId: UUID, key: IdempotencyKey, requestHash: String): AnalysisJob? {
        val existing = analysisJobRepository.findByUserIdAndIdempotencyKey(participantId.toString(), key.value)
            ?: return null
        if (!existing.matchesRequestHash(requestHash)) throw SantaCutException(ErrorCode.IDEMPOTENCY_CONFLICT)
        return existing
    }

    fun enqueue(
        participantId: UUID,
        key: IdempotencyKey,
        submission: AnalysisSubmission,
        payload: AnalysisPayload,
        now: Instant,
    ): AnalysisJob {
        verifyWithinDailyLimit(participantId, now)
        return analysisJobRepository.save(AnalysisJob.enqueue(participantId, key, submission, payload, now))
    }

    /** 원본 파일로 받은 작업을 검증 상태로 저장한다. 하루 제출 한도는 JSON 제출과 함께 센다. */
    fun receiveUpload(
        participantId: UUID,
        key: IdempotencyKey,
        source: DataSource,
        requestHash: String,
        now: Instant,
    ): AnalysisJob {
        verifyWithinDailyLimit(participantId, now)
        return analysisJobRepository.save(AnalysisJob.receive(participantId, key, source, requestHash, now))
    }

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
        job.startAttempt(lease, now)
        return analysisJobRepository.save(job)
    }

    /** 아직 이 리스를 쥐고 실행 중인 작업. 다른 작업자가 가져갔으면 null이다. */
    fun findHeld(jobId: String, lease: String): AnalysisJob? =
        analysisJobRepository.findById(jobId).orElse(null)?.takeIf { it.isHeldBy(lease) }

    fun succeed(job: AnalysisJob, assessmentId: String, finishedAt: Instant) {
        job.succeed(assessmentId, finishedAt)
        analysisJobRepository.save(job)
    }

    fun fail(job: AnalysisJob, code: AnalysisFailureCode, finishedAt: Instant) {
        job.fail(code, finishedAt)
        analysisJobRepository.save(job)
    }

    /**
     * 검증 중인 작업의 검증을 한 번 더 시작한다. 작업이 없거나 이미 끝났으면 null이다.
     * 시도를 다 써서 [AnalysisFailureCode.RETRY_EXHAUSTED]로 끝났어도 저장하고 null을 돌려준다.
     */
    fun startValidation(jobId: String, now: Instant): AnalysisJob? {
        val job = findValidating(jobId) ?: return null
        job.startValidation(now)
        return analysisJobRepository.save(job).takeIf { it.isValidating() }
    }

    /** 검증을 통과한 작업에 정규화한 제출 내용을 담아 대기열에 넣는다. 그사이 끝난 작업은 건드리지 않는다. */
    fun queueValidated(jobId: String, submission: AnalysisSubmission) {
        val job = findValidating(jobId) ?: return
        job.queueValidated(toPayload(submission))
        analysisJobRepository.save(job)
    }

    /** 검증에 실패한 작업을 끝낸다. 그사이 끝난 작업은 건드리지 않는다. */
    fun rejectUpload(jobId: String, code: AnalysisFailureCode, finishedAt: Instant) {
        val job = findValidating(jobId) ?: return
        job.rejectUpload(code, finishedAt)
        analysisJobRepository.save(job)
    }

    private fun expire(job: AnalysisJob, now: Instant) {
        job.expire(now)
        analysisJobRepository.save(job)
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
}
