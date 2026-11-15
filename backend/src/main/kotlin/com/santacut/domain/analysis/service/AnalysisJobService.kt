package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisJobStatus
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
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

    /** 완료 시점에 시즌이 마감됐으면 작업을 시즌 마감 실패로 저장하고 참을 돌려준다. */
    fun freezeIfSeasonClosed(job: AnalysisJob, completedAt: Instant): Boolean {
        val frozen = job.freezeIfSeasonClosed(season(), completedAt)
        if (frozen) analysisJobRepository.save(job)
        return frozen
    }

    /** 같은 키로 이미 받은 작업. 본문이 다르면 충돌이다. */
    fun findReplay(participantId: UUID, key: IdempotencyKey, payload: AnalysisPayload): AnalysisJob? {
        val existing = analysisJobRepository.findByUserIdAndIdempotencyKey(participantId.toString(), key.value)
            ?: return null
        if (!existing.matchesRequest(payload)) throw SantaCutException(ErrorCode.IDEMPOTENCY_CONFLICT)
        return existing
    }

    fun enqueue(
        participantId: UUID,
        key: IdempotencyKey,
        submission: AnalysisSubmission,
        payload: AnalysisPayload,
        now: Instant,
    ): AnalysisJob {
        val userId = participantId.toString()
        val windowStart = AnalysisJob.submissionWindowStart(now)
        AnalysisJob.verifyWithinDailyLimit(
            analysisJobRepository.countByUserIdAndCreatedAtAfter(userId, windowStart),
            analysisJobRepository.findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, windowStart)?.createdAt,
        )
        return analysisJobRepository.save(AnalysisJob.enqueue(participantId, key, submission, payload, now))
    }

    fun getOwned(jobId: UUID, participantId: UUID): AnalysisJob =
        analysisJobRepository.findByIdAndUserId(jobId.toString(), participantId.toString())
            ?: throw SantaCutException(ErrorCode.NOT_FOUND)

    /** 참가자가 가장 최근에 낸 작업. 낸 작업이 없으면 [ErrorCode.NOT_FOUND]다. */
    fun getLatestOwned(participantId: UUID): AnalysisJob =
        analysisJobRepository.findFirstByUserIdOrderByCreatedAtDesc(participantId.toString())
            ?: throw SantaCutException(ErrorCode.NOT_FOUND)

    /** 너무 오래 기다린 대기 작업과 재시도를 다 쓴 실행 작업을 하나씩 실패로 정리한다. */
    fun expireOverdueJobs(now: Instant) {
        analysisJobRepository.findFirstByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            AnalysisJobStatus.QUEUED, AnalysisJob.queueExpiryCutoff(now),
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

    private fun expire(job: AnalysisJob, now: Instant) {
        job.expire(now)
        analysisJobRepository.save(job)
    }

    private fun season(): Season = Season(appProperties.seasonYear)
}
