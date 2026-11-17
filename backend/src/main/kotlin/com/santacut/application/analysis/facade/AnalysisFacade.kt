package com.santacut.application.analysis.facade

import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.LatestAnalysisCommand
import com.santacut.application.analysis.command.OpenAnalysisStreamCommand
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.command.UploadAnalysisCommand
import com.santacut.application.analysis.result.AnalysisStatusChangeResult
import com.santacut.application.analysis.result.AnalysisStatusResult
import com.santacut.application.analysis.result.AnalysisSubmissionResult
import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisJobChange
import com.santacut.domain.analysis.model.AnalysisProgress
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.analysis.service.AnalysisUploadService
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.ranking.service.RankingService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Facade
class AnalysisFacade(
    private val analysisJobService: AnalysisJobService,
    private val participantService: ParticipantService,
    private val analysisScoringService: AnalysisScoringService,
    private val analysisUploadService: AnalysisUploadService,
    private val rankingService: RankingService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(AnalysisFacade::class.java)

    /**
     * 분석 작업을 대기열에 넣는다. 같은 키와 같은 본문으로 다시 보내면 처음 작업을 그대로 돌려준다.
     */
    @Transactional
    fun submit(command: SubmitAnalysisCommand): AnalysisSubmissionResult {
        val idempotencyKey = IdempotencyKey.from(command.idempotencyKey)
        val submission = command.toSubmission()
        val now = clock.instant()
        analysisJobService.verifyAcceptable(submission, now)
        val payload = analysisJobService.toPayload(submission)
        participantService.verifyRegistered(command.participantId)
        val replay = analysisJobService.findReplay(command.participantId, idempotencyKey, payload)
        if (replay != null) return AnalysisSubmissionResult.from(replay, progressOf(replay, command.participantId))
        analysisScoringService.verifyEnabled()
        analysisJobService.verifyAcceptingSubmission(command.participantId, command.clientIp, now)
        val job = analysisJobService.enqueue(command.participantId, idempotencyKey, submission, payload, now)
        return AnalysisSubmissionResult.from(job, progressOf(job, command.participantId))
    }

    /**
     * 원본 파일로 분석 작업을 받는다. 최대 20MB인 내용을 읽기 전에 시즌, 참가자, 같은 키의 작업, 제출 한도부터 확인해
     * 거절할 요청이 원본을 메모리에 올리지 않게 한다. 한도 확인과 내용 읽기·저장은 동시 처리 상한 안에서 한다.
     * 상한에 걸려 UPLOAD_BUSY로 돌려보낸 요청이 IP 제출 한도를 쓰지 않게 하려고 자리부터 잡는다.
     * 같은 키로 같은 파일·출처·본인 이름을 다시 보내면 처음 작업을 그대로 돌려준다.
     */
    @Transactional
    fun upload(command: UploadAnalysisCommand): AnalysisSubmissionResult {
        val idempotencyKey = IdempotencyKey.from(command.idempotencyKey)
        val now = clock.instant()
        rankingService.verifySeasonOpenAt(now)
        participantService.verifyRegistered(command.participantId)
        val existing = analysisJobService.findByKey(command.participantId, idempotencyKey)
        val job = analysisUploadService.withinConcurrencyLimit(now) {
            if (existing == null) verifyNewUploadAcceptable(command, now)
            receiveUpload(command, idempotencyKey, existing, now)
        }
        return AnalysisSubmissionResult.from(job, progressOf(job, command.participantId))
    }

    /** 작업 하나의 현재 진행 단계. 화면은 끝날 때까지 이 조회를 반복한다. */
    @Transactional(readOnly = true)
    fun getStatus(command: AnalysisJobCommand): AnalysisStatusResult {
        val job = analysisJobService.getOwned(command.jobId, command.participantId)
        return AnalysisStatusResult.from(job, progressOf(job, command.participantId))
    }

    /** 참가자가 가장 최근에 낸 작업의 진행 단계. 재방문 때 이어 볼 작업을 찾는다. */
    @Transactional(readOnly = true)
    fun getLatestStatus(command: LatestAnalysisCommand): AnalysisStatusResult {
        val job = analysisJobService.getLatestOwned(command.participantId)
        return AnalysisStatusResult.from(job, progressOf(job, command.participantId))
    }

    /**
     * 진행 상황 스트림을 하나 더 열 수 있는지 확인한다. 상한에 걸리면 ANALYSIS_STREAM_LIMIT(503)이고, 화면은 폴링으로 바꾼다.
     * 연결하자마자 보낼 현재 상태는 스트림을 등록한 뒤 [getStatus]로 읽는다.
     */
    fun verifyStreamOpenable(command: OpenAnalysisStreamCommand) {
        analysisJobService.verifyStreamOpenable(command.openStreams, command.replacing, clock.instant())
    }

    /**
     * 작업 행을 저장한 트랜잭션이 커밋된 뒤 응답용 새 상태를 만들어 진행 상황 스트림에 알린다.
     * 커밋 뒤 단계라 원래 트랜잭션 자원으로 읽기만 한다. 실패해도 작업 흐름을 막지 않도록 로그만 남긴다.
     */
    @TransactionalEventListener(fallbackExecution = true)
    fun announceStatusChange(change: AnalysisJobChange) {
        try {
            val job = analysisJobService.find(change.jobId) ?: return
            val participantId = UUID.fromString(job.userId)
            val status = AnalysisStatusResult.from(job, progressOf(job, participantId))
            eventPublisher.publishEvent(AnalysisStatusChangeResult(participantId, status))
        } catch (exception: RuntimeException) {
            logger.warn("분석 작업 {} 상태 알림 실패: {}", change.jobId, exception.javaClass.simpleName)
        }
    }

    private fun verifyNewUploadAcceptable(command: UploadAnalysisCommand, now: Instant) {
        analysisScoringService.verifyEnabled()
        analysisJobService.verifyAcceptingSubmission(command.participantId, command.clientIp, now)
    }

    /**
     * 원본을 한 번 읽어 명백히 대화 파일이 아니면 거절하고(나머지 검증은 검증 작업자가 한다), 검증 대기 작업으로 저장한다.
     * 같은 키로 받은 작업([existing])이 있으면 요청 해시가 같을 때 그 작업을 돌려주고 다르면 충돌이다.
     */
    private fun receiveUpload(
        command: UploadAnalysisCommand,
        key: IdempotencyKey,
        existing: AnalysisJob?,
        now: Instant,
    ): AnalysisJob {
        val file = command.readRawFile().also { it.verifyReceivable() }
        val requestHash = file.requestHash(command.selfSpeaker)
        if (existing != null) return analysisJobService.verifyReplay(existing, requestHash)
        val job = analysisJobService.receiveUpload(command.participantId, key, file.source, requestHash, now)
        analysisUploadService.store(job, command.selfSpeaker, file, now)
        return job
    }

    private fun progressOf(job: AnalysisJob, participantId: UUID): AnalysisProgress =
        job.progress(rankingService.findRankedAssessmentTime(participantId))
}
