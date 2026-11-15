package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.ranking.model.Season
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalysisJobTest {
    private val createdAt = Instant.parse("2026-11-15T00:00:00Z")
    private val payload = AnalysisPayload("{\"source\":\"KAKAO\",\"events\":[]}")

    @Test
    fun `새 작업은 대기 상태로 요청 해시와 제출 원문을 가진다`() {
        val job = newJob()
        assertEquals(AnalysisJobStatus.QUEUED, job.status)
        assertEquals(0, job.attempts)
        assertTrue(job.matchesRequest(payload))
        assertFalse(job.matchesRequest(AnalysisPayload("{}")))
        assertEquals(payload.json, job.requirePayload())
    }

    @Test
    fun `시도를 시작하면 실행 상태가 되고 리스를 3분 동안 쥔다`() {
        val job = newJob()
        job.startAttempt(LEASE, createdAt)
        assertEquals(AnalysisJobStatus.RUNNING, job.status)
        assertEquals(1, job.attempts)
        assertEquals(createdAt.plus(Duration.ofMinutes(3)), job.leaseUntil)
        assertTrue(job.isHeldBy(LEASE))
        assertFalse(job.isHeldBy("other-lease"))
    }

    @Test
    fun `성공하면 평가 ID를 기록하고 제출 원문을 지운다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt) }
        val finishedAt = createdAt.plusSeconds(30)
        job.succeed("assessment-1", finishedAt)
        assertEquals(AnalysisJobStatus.SUCCEEDED, job.status)
        assertEquals("assessment-1", job.assessmentId)
        assertEquals(finishedAt, job.finishedAt)
        assertNull(job.payload)
        assertTrue(job.hasSucceeded())
        assertFalse(job.isHeldBy(LEASE))
    }

    @Test
    fun `근거 부족으로 끝나면 INSUFFICIENT_DATA 상태와 INSUFFICIENT_EVIDENCE 코드가 된다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt) }
        job.fail(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, createdAt)
        assertEquals(AnalysisJobStatus.INSUFFICIENT_DATA, job.status)
        assertEquals(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, job.errorCode)
        assertTrue(job.hasFailed())
        assertNull(job.payload)
    }

    @Test
    fun `그 밖의 실패 이유는 모두 FAILED 상태가 된다`() {
        val codes = listOf(
            AnalysisFailureCode.SEASON_FROZEN, AnalysisFailureCode.EXPIRED,
            AnalysisFailureCode.RETRY_EXHAUSTED, AnalysisFailureCode.ANALYSIS_FAILED,
        )
        codes.forEach { code ->
            val job = newJob().apply { startAttempt(LEASE, createdAt) }
            job.fail(code, createdAt)
            assertEquals(AnalysisJobStatus.FAILED, job.status)
            assertEquals(code, job.errorCode)
        }
    }

    @Test
    fun `오래 기다린 대기 작업은 EXPIRED로 끝나고 기준 시각은 지금부터 24시간 전이다`() {
        val job = newJob()
        job.expire(createdAt)
        assertEquals(AnalysisJobStatus.FAILED, job.status)
        assertEquals(AnalysisFailureCode.EXPIRED, job.errorCode)
        assertNull(job.payload)
        assertEquals(createdAt.minus(Duration.ofHours(24)), AnalysisJob.queueExpiryCutoff(createdAt))
    }

    @Test
    fun `리스가 끝난 실행 작업은 다시 잡을 수 있지만 리스가 남았으면 잡을 수 없다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt) }
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.startAttempt("other-lease", createdAt) }
        val afterLease = createdAt.plus(Duration.ofMinutes(3)).plusMillis(1)
        assertTrue(job.isClaimableAt(afterLease))
        job.startAttempt("other-lease", afterLease)
        assertEquals(2, job.attempts)
        assertTrue(job.isHeldBy("other-lease"))
    }

    @Test
    fun `끝난 작업은 다시 잡을 수 없다`() {
        val job = newJob().apply { expire(createdAt) }
        assertFalse(job.isClaimableAt(createdAt))
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.startAttempt(LEASE, createdAt) }
    }

    @Test
    fun `실행 중이 아닌 작업은 성공이나 실패로 끝낼 수 없다`() {
        val queued = newJob()
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { queued.succeed("assessment-1", createdAt) }
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) {
            queued.fail(AnalysisFailureCode.ANALYSIS_FAILED, createdAt)
        }
        val succeeded = newJob().apply { startAttempt(LEASE, createdAt) }
        succeeded.succeed("assessment-1", createdAt)
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) {
            succeeded.fail(AnalysisFailureCode.ANALYSIS_FAILED, createdAt)
        }
        assertEquals(AnalysisJobStatus.SUCCEEDED, succeeded.status)
    }

    @Test
    fun `대기 중이 아닌 작업은 만료할 수 없다`() {
        val running = newJob().apply { startAttempt(LEASE, createdAt) }
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { running.expire(createdAt) }
        assertEquals(AnalysisJobStatus.RUNNING, running.status)
    }

    @Test
    fun `점수 산정 예외 중 근거 부족만 따로 구분한다`() {
        val insufficient = SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)
        assertEquals(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, AnalysisFailureCode.fromScoringFailure(insufficient))
        val invalid = SantaCutException(ErrorCode.AI_RESPONSE_INVALID)
        assertEquals(AnalysisFailureCode.ANALYSIS_FAILED, AnalysisFailureCode.fromScoringFailure(invalid))
        assertEquals(AnalysisFailureCode.ANALYSIS_FAILED, AnalysisFailureCode.fromScoringFailure(IllegalStateException()))
    }

    @Test
    fun `진행 단계는 대기, 실행, 실패를 응답 어휘로 바꾼다`() {
        val job = newJob()
        assertEquals(AnalysisProgress.QUEUED, job.progress(null))
        job.startAttempt(LEASE, createdAt)
        assertEquals(AnalysisProgress.ANALYZING, job.progress(null))
        job.fail(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, createdAt)
        assertEquals(AnalysisProgress.FAILED, job.progress(null))
    }

    @Test
    fun `성공한 작업은 완료 시각 이후 순위가 계산됐을 때만 성공이고 아니면 순위 계산 대기다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt) }
        val finishedAt = createdAt.plusSeconds(30)
        job.succeed("assessment-1", finishedAt)
        assertEquals(AnalysisProgress.RANKING_PENDING, job.progress(null))
        assertEquals(AnalysisProgress.RANKING_PENDING, job.progress(finishedAt.minusMillis(1)))
        assertEquals(AnalysisProgress.SUCCEEDED, job.progress(finishedAt))
        assertEquals(AnalysisProgress.SUCCEEDED, job.progress(finishedAt.plusSeconds(60)))
    }

    @Test
    fun `제출 한도는 지금부터 하루 전까지 낸 작업을 세고 세 번째까지만 받는다`() {
        assertEquals(createdAt.minus(Duration.ofDays(1)), AnalysisJob.submissionWindowStart(createdAt))
        AnalysisJob.verifyWithinDailyLimit(2, createdAt)
        val exception = assertFailsWith<SantaCutException> { AnalysisJob.verifyWithinDailyLimit(3, createdAt) }
        assertEquals(ErrorCode.DAILY_USER_LIMIT, exception.errorCode)
    }

    @Test
    fun `제출 한도를 넘으면 기간 안 가장 오래된 제출에서 하루 뒤를 다시 낼 수 있는 시각으로 알린다`() {
        val exception = assertFailsWith<SantaCutException> { AnalysisJob.verifyWithinDailyLimit(3, createdAt) }
        assertEquals(createdAt.plus(Duration.ofDays(1)), exception.retryAt)
    }

    @Test
    fun `완료 시점에 시즌이 마감됐으면 시즌 마감 실패로 끝낸다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt) }
        val christmas = Season(2026).deliveryStart().toInstant()
        assertTrue(job.freezeIfSeasonClosed(Season(2026), christmas))
        assertEquals(AnalysisJobStatus.FAILED, job.status)
        assertEquals(AnalysisFailureCode.SEASON_FROZEN, job.errorCode)
        assertEquals(christmas, job.finishedAt)
    }

    @Test
    fun `완료 시점에 시즌이 열려 있으면 상태를 바꾸지 않는다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt) }
        assertFalse(job.freezeIfSeasonClosed(Season(2026), createdAt))
        assertEquals(AnalysisJobStatus.RUNNING, job.status)
        assertNull(job.errorCode)
    }

    private fun newJob(): AnalysisJob = AnalysisJob.enqueue(
        UUID.randomUUID(),
        IdempotencyKey.from("key-0123456789abcdef"),
        AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
        payload,
        createdAt,
    )

    companion object {
        private const val LEASE = "lease-1"
    }
}
