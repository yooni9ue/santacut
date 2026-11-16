package com.santacut.application.analysis.facade

import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.prompt.service.PromptService
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
 *
 * AI를 부를 때마다 채점 프롬프트(`score`)의 최신 판을 DB에서 읽는다. 캐시하지 않으므로 관리자 API로 저장한 새 판이
 * 다음 작업부터 바로 쓰인다. 프롬프트가 하나도 없으면 작업은 PROMPT_NOT_FOUND 사유로 실패한다.
 * 로그에는 프롬프트 이름과 판 번호만 남긴다.
 *
 * 앱에는 AI 호출 수 한도가 없다. AI 비용은 AWS Bedrock 예산 차단(santacut-infra)으로만 막는다.
 */
@Facade
class AnalysisWorkerFacade(
    private val analysisJobService: AnalysisJobService,
    private val analysisScoringService: AnalysisScoringService,
    private val assessmentService: AssessmentService,
    private val rankingService: RankingService,
    private val shareService: ShareService,
    private val promptService: PromptService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(AnalysisWorkerFacade::class.java)

    @Scheduled(fixedDelayString = "\${santacut.ai.poll-delay-ms:${DEFAULT_POLL_DELAY_MS}}")
    fun processNextJob() {
        // AI가 꺼져 있어도 24시간 넘게 기다린 작업은 정리한다(기획 §4.6). 리스 획득과 따로 짧은 트랜잭션으로 연다.
        transactionTemplate.executeWithoutResult { analysisJobService.expireOverdueJobs(clock.instant()) }
        if (!analysisScoringService.isEnabled()) return
        val lease = UUID.randomUUID().toString()
        val job = transactionTemplate.execute { claimNextJob(lease) } ?: return
        try {
            val prompt = promptService.latest(PromptName.SCORE)
            logger.info("분석 작업 {} 채점: 프롬프트 {} {}판", job.id, prompt.name, prompt.version)
            val score = analysisScoringService.score(job, prompt.content)
            transactionTemplate.executeWithoutResult { complete(job.id, lease, score, prompt.version) }
        } catch (exception: Exception) {
            // 제공자 응답 본문이나 제출 메시지는 로그에 남기지 않는다. 실패한 작업은 새 요청으로만 다시 시도한다.
            logger.warn("분석 작업 {} 실패: {}", job.id, failureReason(exception))
            transactionTemplate.executeWithoutResult { failIfHeld(job.id, lease, exception) }
        }
    }

    /** 꺼낼 수 있는 다음 작업의 리스를 잡는다. */
    private fun claimNextJob(lease: String): AnalysisJob? {
        val now = clock.instant()
        val job = analysisJobService.findClaimable(now) ?: return null
        return analysisJobService.startAttempt(job, lease, now)
    }

    /**
     * 평가를 저장하고 같은 트랜잭션에서 순위를 다시 계산한다. 순위 계산이 실패하면 둘 다 롤백된다.
     * 시즌이 이미 마감됐으면 작업 쪽 규칙에 따라 실패로 끝내고 평가를 저장하지 않는다.
     */
    private fun complete(jobId: String, lease: String, score: Score, promptVersion: Int) {
        val job = analysisJobService.findHeld(jobId, lease) ?: return
        val completedAt = clock.instant()
        if (analysisJobService.freezeIfSeasonClosed(job, completedAt)) return
        val assessment = assessmentService.record(job, score, promptVersion, completedAt)
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
