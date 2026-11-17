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
        assertTrue(job.matchesRequestHash(payload.hash()))
        assertFalse(job.matchesRequestHash(AnalysisPayload("{}").hash()))
        assertEquals(payload.json, job.requirePayload())
    }

    @Test
    fun `시도를 시작하면 실행 상태가 되고 구간 수 상한만큼의 호출 시간에 여유를 더한 동안 리스를 쥔다`() {
        val job = newJob()
        job.startAttempt(LEASE, createdAt, LEASE_DURATION)
        assertEquals(AnalysisJobStatus.RUNNING, job.status)
        assertEquals(1, job.attempts)
        assertEquals(createdAt.plus(Duration.ofMinutes(7)), job.leaseUntil)
        assertTrue(job.isHeldBy(LEASE))
        assertFalse(job.isHeldBy("other-lease"))
    }

    @Test
    fun `성공하면 평가 ID를 기록하고 제출 원문을 지운다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        val finishedAt = createdAt.plusSeconds(30)
        job.succeed("assessment-1", finishedAt)
        assertEquals(AnalysisJobStatus.SUCCEEDED, job.status)
        assertEquals("assessment-1", job.assessmentId)
        assertEquals(finishedAt, job.finishedAt)
        assertNull(job.payload)
        assertFalse(job.isHeldBy(LEASE))
    }

    @Test
    fun `근거 부족으로 끝나면 INSUFFICIENT_DATA 상태와 INSUFFICIENT_EVIDENCE 코드가 된다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        job.fail(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, createdAt)
        assertEquals(AnalysisJobStatus.INSUFFICIENT_DATA, job.status)
        assertEquals(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, job.errorCode)
        assertNull(job.payload)
    }

    @Test
    fun `그 밖의 실패 이유는 모두 FAILED 상태가 된다`() {
        val codes = listOf(
            AnalysisFailureCode.SEASON_FROZEN, AnalysisFailureCode.EXPIRED,
            AnalysisFailureCode.RETRY_EXHAUSTED, AnalysisFailureCode.ANALYSIS_FAILED,
        )
        codes.forEach { code ->
            val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
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
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.startAttempt("other-lease", createdAt, LEASE_DURATION) }
        val afterLease = createdAt.plus(LEASE_DURATION).plusMillis(1)
        assertTrue(job.isClaimableAt(afterLease))
        job.startAttempt("other-lease", afterLease, LEASE_DURATION)
        assertEquals(2, job.attempts)
        assertTrue(job.isHeldBy("other-lease"))
    }

    @Test
    fun `끝난 작업은 다시 잡을 수 없다`() {
        val job = newJob().apply { expire(createdAt) }
        assertFalse(job.isClaimableAt(createdAt))
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.startAttempt(LEASE, createdAt, LEASE_DURATION) }
    }

    @Test
    fun `실행 중이 아닌 작업은 성공이나 실패로 끝낼 수 없다`() {
        val queued = newJob()
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { queued.succeed("assessment-1", createdAt) }
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) {
            queued.fail(AnalysisFailureCode.ANALYSIS_FAILED, createdAt)
        }
        val succeeded = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        succeeded.succeed("assessment-1", createdAt)
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) {
            succeeded.fail(AnalysisFailureCode.ANALYSIS_FAILED, createdAt)
        }
        assertEquals(AnalysisJobStatus.SUCCEEDED, succeeded.status)
    }

    @Test
    fun `대기 중이 아닌 작업은 만료할 수 없다`() {
        val running = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
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
        job.startAttempt(LEASE, createdAt, LEASE_DURATION)
        assertEquals(AnalysisProgress.ANALYZING, job.progress(null))
        job.fail(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, createdAt)
        assertEquals(AnalysisProgress.FAILED, job.progress(null))
    }

    @Test
    fun `성공한 작업은 완료 시각 이후 순위가 계산됐을 때만 성공이고 아니면 순위 계산 대기다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
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
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        val christmas = Season(2026).deliveryStart().toInstant()
        assertTrue(job.freezeIfSeasonClosed(Season(2026), christmas))
        assertEquals(AnalysisJobStatus.FAILED, job.status)
        assertEquals(AnalysisFailureCode.SEASON_FROZEN, job.errorCode)
        assertEquals(christmas, job.finishedAt)
    }

    @Test
    fun `완료 시점에 시즌이 열려 있으면 상태를 바꾸지 않는다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        assertFalse(job.freezeIfSeasonClosed(Season(2026), createdAt))
        assertEquals(AnalysisJobStatus.RUNNING, job.status)
        assertNull(job.errorCode)
    }

    @Test
    fun `원본 파일로 받은 작업은 페이로드 없이 검증 상태로 시작하고 파일 해시로 재전송을 가린다`() {
        val job = newUpload()
        assertEquals(AnalysisJobStatus.VALIDATING, job.status)
        assertEquals(AnalysisProgress.VALIDATING, job.progress(null))
        assertNull(job.payload)
        assertTrue(job.matchesRequestHash(FILE_HASH))
        assertFalse(job.matchesRequestHash("other-hash"))
        assertFalse(job.isClaimableAt(createdAt))
    }

    @Test
    fun `검증을 통과하면 정규화한 페이로드를 담아 대기열에 넣고 시도 횟수를 처음부터 센다`() {
        val job = newUpload().apply { startValidation(createdAt) }
        assertEquals(1, job.attempts)
        job.queueValidated(payload)
        assertEquals(AnalysisJobStatus.QUEUED, job.status)
        assertEquals(0, job.attempts)
        assertEquals(payload.json, job.requirePayload())
        assertTrue(job.matchesRequestHash(FILE_HASH))
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.queueValidated(payload) }
    }

    @Test
    fun `검증에 실패하면 실패 이유에 맞는 상태로 끝난다`() {
        val unsupported = newUpload().apply { rejectUpload(AnalysisFailureCode.UNSUPPORTED_FILE_FORMAT, createdAt) }
        assertEquals(AnalysisJobStatus.FAILED, unsupported.status)
        assertEquals(AnalysisFailureCode.UNSUPPORTED_FILE_FORMAT, unsupported.errorCode)
        assertEquals(createdAt, unsupported.finishedAt)
        val insufficient = newUpload().apply { rejectUpload(AnalysisFailureCode.INSUFFICIENT_EVENTS, createdAt) }
        assertEquals(AnalysisJobStatus.INSUFFICIENT_DATA, insufficient.status)
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) {
            newJob().rejectUpload(AnalysisFailureCode.UNSUPPORTED_FILE_FORMAT, createdAt)
        }
    }

    @Test
    fun `검증을 세 번 넘게 시작하면 RETRY_EXHAUSTED로 끝난다`() {
        val job = newUpload()
        repeat(AnalysisJob.MAX_ATTEMPTS) { job.startValidation(createdAt) }
        assertTrue(job.isValidating())
        job.startValidation(createdAt)
        assertEquals(AnalysisJobStatus.FAILED, job.status)
        assertEquals(AnalysisFailureCode.RETRY_EXHAUSTED, job.errorCode)
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.startValidation(createdAt) }
    }

    @Test
    fun `검증 중인 작업도 오래 기다리면 만료한다`() {
        val job = newUpload().apply { expire(createdAt) }
        assertEquals(AnalysisJobStatus.FAILED, job.status)
        assertEquals(AnalysisFailureCode.EXPIRED, job.errorCode)
    }

    @Test
    fun `검증 예외는 형식, 화자, 이벤트 부족만 따로 구분한다`() {
        val expected = mapOf(
            ErrorCode.UNSUPPORTED_FILE_FORMAT to AnalysisFailureCode.UNSUPPORTED_FILE_FORMAT,
            ErrorCode.SPEAKER_NOT_IDENTIFIED to AnalysisFailureCode.SPEAKER_NOT_IDENTIFIED,
            ErrorCode.INSUFFICIENT_EVENTS to AnalysisFailureCode.INSUFFICIENT_EVENTS,
            ErrorCode.INVALID_EVENT_RANGE to AnalysisFailureCode.ANALYSIS_FAILED,
        )
        expected.forEach { (errorCode, failureCode) ->
            assertEquals(failureCode, AnalysisFailureCode.fromValidationFailure(SantaCutException(errorCode)))
        }
        assertEquals(AnalysisFailureCode.ANALYSIS_FAILED, AnalysisFailureCode.fromValidationFailure(IllegalStateException()))
    }

    @Test
    fun `제공자가 호출을 막으면 리스를 풀고 대기열로 되돌리며 이번 시도는 세지 않는다`() {
        val job = newJob()
        job.startAttempt(LEASE, createdAt, LEASE_DURATION)

        job.releaseLease()

        assertEquals(AnalysisJobStatus.QUEUED, job.status)
        assertEquals(0, job.attempts)
        assertNull(job.leaseUntil)
        assertFalse(job.isHeldBy(LEASE))
        assertEquals(payload.json, job.requirePayload())
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.releaseLease() }
    }

    @Test
    fun `써 버린 시도는 센 채로 대기열로 되돌리고 시도를 다 썼으면 RETRY_EXHAUSTED로 끝내며 제출 원문을 지운다`() {
        val job = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }

        job.requeueSpentAttempt(createdAt)

        assertEquals(AnalysisJobStatus.QUEUED, job.status)
        assertEquals(1, job.attempts)
        assertFalse(job.isHeldBy(LEASE))
        repeat(AnalysisJob.MAX_ATTEMPTS - 1) {
            assertEquals(AnalysisJobStatus.QUEUED, job.status)
            job.startAttempt(LEASE, createdAt, LEASE_DURATION)
            job.requeueSpentAttempt(createdAt)
        }
        assertEquals(AnalysisFailureCode.RETRY_EXHAUSTED, job.errorCode)
        assertNull(job.payload)
        assertErrorCode(ErrorCode.ANALYSIS_JOB_STATE_INVALID) { job.requeueSpentAttempt(createdAt) }
    }

    @Test
    fun `채점 입력 해시와 함께 끝낸 작업은 다시 쓸 결과를 알려 준다`() {
        val succeeded = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }
        val failed = newJob().apply { startAttempt(LEASE, createdAt, LEASE_DURATION) }

        succeeded.succeed("assessment-1", createdAt, DIGEST_HASH)
        failed.fail(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, createdAt, DIGEST_HASH)

        assertEquals(DIGEST_HASH, succeeded.digestHash)
        assertEquals("assessment-1", succeeded.reusableAssessmentId())
        assertNull(succeeded.reusableFailure())
        assertNull(failed.reusableAssessmentId())
        assertEquals(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, failed.reusableFailure())
    }

    @Test
    fun `근거 부족과 응답 오류만 같은 입력에 다시 쓸 수 있는 실패다`() {
        val reusable = listOf(
            ErrorCode.INSUFFICIENT_EVIDENCE, ErrorCode.AI_RESPONSE_INVALID, ErrorCode.AI_RESPONSE_SCHEMA_INVALID,
            ErrorCode.AI_EVIDENCE_INVALID, ErrorCode.AI_SCORE_OUT_OF_RANGE,
        )
        reusable.forEach { assertTrue(AnalysisFailureCode.isReusableScoringFailure(SantaCutException(it)), it.name) }
        assertFalse(AnalysisFailureCode.isReusableScoringFailure(SantaCutException(ErrorCode.AI_PROVIDER_UNAVAILABLE)))
        assertFalse(AnalysisFailureCode.isReusableScoringFailure(IllegalStateException()))
    }

    private fun newUpload(): AnalysisJob = AnalysisJob.receive(
        UUID.randomUUID(),
        IdempotencyKey.from("key-0123456789abcdef"),
        DataSource.KAKAO,
        FILE_HASH,
        createdAt,
    )

    private fun newJob(): AnalysisJob = AnalysisJob.enqueue(
        UUID.randomUUID(),
        IdempotencyKey.from("key-0123456789abcdef"),
        AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
        payload,
        createdAt,
    )

    companion object {
        private const val LEASE = "lease-1"
        private const val FILE_HASH = "file-hash"
        private const val DIGEST_HASH = "digest-hash"
        private val LEASE_DURATION: Duration = AnalysisJob.leaseDurationFor(Duration.ofSeconds(60))
    }
}
