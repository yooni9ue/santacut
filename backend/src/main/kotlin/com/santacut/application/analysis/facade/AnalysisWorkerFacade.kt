package com.santacut.application.analysis.facade

import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.service.AiBudgetService
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.share.service.ShareService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.util.UUID

/**
 * 대기 중인 분석 작업을 하나씩 꺼내 AI 점수를 매기고 순위를 다시 계산하는 백그라운드 작업자.
 *
 * 리스 획득, AI 호출, 결과 기록을 한 트랜잭션으로 묶으면 느린 AI 호출 동안 SQLite 쓰기 잠금을 쥐게 된다.
 * 그래서 이 facade만 [TransactionTemplate]으로 리스 획득과 결과 기록 트랜잭션을 따로 나눈다.
 */
@Facade
class AnalysisWorkerFacade(
    private val analysisJobService: AnalysisJobService,
    private val aiBudgetService: AiBudgetService,
    private val analysisScoringService: AnalysisScoringService,
    private val assessmentService: AssessmentService,
    private val rankingService: RankingService,
    private val shareService: ShareService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(AnalysisWorkerFacade::class.java)

    @Scheduled(fixedDelayString = "\${santacut.ai.poll-delay-ms:${DEFAULT_POLL_DELAY_MS}}")
    fun processNextJob() {
        if (!analysisScoringService.isEnabled()) return
        val lease = UUID.randomUUID().toString()
        val job = transactionTemplate.execute { claimNextJob(lease) } ?: return
        try {
            val score = analysisScoringService.score(job)
            transactionTemplate.executeWithoutResult { complete(job.id, lease, score) }
        } catch (exception: Exception) {
            // 제공자 응답 본문이나 제출 메시지는 로그에 남기지 않는다. 실패한 작업은 새 요청으로만 다시 시도한다.
            logger.warn("분석 작업 {} 실패: {}", job.id, failureReason(exception))
            transactionTemplate.executeWithoutResult { failIfHeld(job.id, lease, exception) }
        }
    }

    /** 오래된 작업을 정리하고, 오늘 AI 예산이 남았으면 다음 작업의 리스를 잡는다. */
    private fun claimNextJob(lease: String): AnalysisJob? {
        val now = clock.instant()
        analysisJobService.expireOverdueJobs(now)
        val job = analysisJobService.findClaimable(now) ?: return null
        if (!aiBudgetService.tryConsume(now)) return null
        return analysisJobService.startAttempt(job, lease, now)
    }

    /**
     * 평가를 저장하고 같은 트랜잭션에서 순위를 다시 계산한다. 순위 계산이 실패하면 둘 다 롤백된다.
     * 시즌이 이미 마감됐으면 작업 쪽 규칙에 따라 실패로 끝내고 평가를 저장하지 않는다.
     */
    private fun complete(jobId: String, lease: String, score: Score) {
        val job = analysisJobService.findHeld(jobId, lease) ?: return
        val completedAt = clock.instant()
        if (analysisJobService.freezeIfSeasonClosed(job, completedAt)) return
        val assessment = assessmentService.record(job, score, completedAt)
        analysisJobService.succeed(job, assessment.id, completedAt)
        rankingService.rebuild(completedAt, shareService.findAll())
    }

    private fun failIfHeld(jobId: String, lease: String, exception: Exception) {
        val job = analysisJobService.findHeld(jobId, lease) ?: return
        analysisJobService.fail(job, AnalysisFailureCode.fromScoringFailure(exception), clock.instant())
    }

    private fun failureReason(exception: Exception): String =
        if (exception is SantaCutException) exception.logReason() else exception.javaClass.simpleName

    companion object {
        /** `santacut.ai.poll-delay-ms`가 없을 때 작업자가 다음 작업을 찾기 전 쉬는 시간(밀리초). */
        private const val DEFAULT_POLL_DELAY_MS = 3000L
    }
}
