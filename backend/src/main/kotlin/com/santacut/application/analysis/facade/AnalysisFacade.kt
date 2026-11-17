package com.santacut.application.analysis.facade

import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.command.UploadAnalysisCommand
import com.santacut.application.analysis.result.AnalysisSubmissionResult
import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.analysis.service.AnalysisUploadService
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.ranking.service.RankingService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** 분석 요청(JSON 제출, 원본 파일)을 받아 작업으로 저장한다. 진행 단계 조회와 알림은 [AnalysisStatusFacade]가 한다. */
@Facade
class AnalysisFacade(
    private val analysisJobService: AnalysisJobService,
    private val participantService: ParticipantService,
    private val analysisScoringService: AnalysisScoringService,
    private val analysisUploadService: AnalysisUploadService,
    private val rankingService: RankingService,
    private val clock: Clock,
) {
    /**
     * 분석 작업을 대기열에 넣는다. 같은 키와 같은 본문으로 다시 보내면 처음 작업을 그대로 돌려준다.
     */
    @Transactional
    fun submit(command: SubmitAnalysisCommand): AnalysisSubmissionResult {
        val idempotencyKey = IdempotencyKey.from(command.idempotencyKey)
        val submission = command.toSubmission()
        val now = clock.instant()
        val payload = acceptSubmission(command.participantId, submission, now)
        val job = analysisJobService.findReplay(command.participantId, idempotencyKey, payload)
            ?: enqueueNew(command, idempotencyKey, submission, payload, now)
        return AnalysisSubmissionResult.from(job, rankingService.progressOf(job, command.participantId))
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
        return AnalysisSubmissionResult.from(job, rankingService.progressOf(job, command.participantId))
    }

    /** 시즌 마감 전이고 이벤트 범위가 맞고 등록한 참가자인지 확인한 뒤, 같은 요청을 알아볼 본문을 만든다. */
    private fun acceptSubmission(participantId: UUID, submission: AnalysisSubmission, now: Instant): AnalysisPayload {
        analysisJobService.verifyAcceptable(submission, now)
        val payload = analysisJobService.toPayload(submission)
        participantService.verifyRegistered(participantId)
        return payload
    }

    /** AI가 켜져 있고 제출 한도 안이면 새 작업을 대기열에 넣는다. */
    private fun enqueueNew(
        command: SubmitAnalysisCommand,
        idempotencyKey: IdempotencyKey,
        submission: AnalysisSubmission,
        payload: AnalysisPayload,
        now: Instant,
    ): AnalysisJob {
        analysisScoringService.verifyEnabled()
        analysisJobService.verifyAcceptingSubmission(command.participantId, command.clientIp, now)
        return analysisJobService.enqueue(command.participantId, idempotencyKey, submission, payload, now)
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
}
