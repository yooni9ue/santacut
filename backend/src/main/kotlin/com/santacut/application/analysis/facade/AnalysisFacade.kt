package com.santacut.application.analysis.facade

import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.LatestAnalysisCommand
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.result.AnalysisStatusResult
import com.santacut.application.analysis.result.AnalysisSubmissionResult
import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisProgress
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.ranking.service.RankingService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Facade
class AnalysisFacade(
    private val analysisJobService: AnalysisJobService,
    private val participantService: ParticipantService,
    private val analysisScoringService: AnalysisScoringService,
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
        analysisJobService.verifyAcceptable(submission, now)
        val payload = analysisJobService.toPayload(submission)
        participantService.verifyRegistered(command.participantId)
        val replay = analysisJobService.findReplay(command.participantId, idempotencyKey, payload)
        if (replay != null) return AnalysisSubmissionResult.from(replay, progressOf(replay, command.participantId))
        analysisScoringService.verifyEnabled()
        val job = analysisJobService.enqueue(command.participantId, idempotencyKey, submission, payload, now)
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

    private fun progressOf(job: AnalysisJob, participantId: UUID): AnalysisProgress =
        job.progress(rankingService.findLatestSnapshot(participantId)?.computedAt)
}
