package com.santacut.application.facade

import com.santacut.application.command.AnalysisJobCommand
import com.santacut.application.command.SubmitAnalysisCommand
import com.santacut.application.result.AnalysisOutcomeResult
import com.santacut.application.result.AnalysisStatusResult
import com.santacut.application.result.AnalysisSubmissionResult
import com.santacut.domain.model.IdempotencyKey
import com.santacut.domain.service.AnalysisJobService
import com.santacut.domain.service.AnalysisScoringService
import com.santacut.domain.service.ParticipantService
import com.santacut.domain.service.RankingService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

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
        if (replay != null) return AnalysisSubmissionResult.from(replay)
        analysisScoringService.verifyEnabled()
        val job = analysisJobService.enqueue(command.participantId, idempotencyKey, submission, payload, now)
        return AnalysisSubmissionResult.from(job)
    }

    @Transactional(readOnly = true)
    fun getStatus(command: AnalysisJobCommand): AnalysisStatusResult =
        AnalysisStatusResult.from(analysisJobService.getOwned(command.jobId, command.participantId))

    /** 성공한 작업의 점수와 그 평가가 반영된 순위. */
    @Transactional(readOnly = true)
    fun getOutcome(command: AnalysisJobCommand): AnalysisOutcomeResult {
        val job = analysisJobService.getOwned(command.jobId, command.participantId)
        return AnalysisOutcomeResult.from(rankingService.findResultOfAssessment(job.requireAssessmentId()))
    }
}
