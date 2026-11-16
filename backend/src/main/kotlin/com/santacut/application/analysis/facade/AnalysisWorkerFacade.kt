package com.santacut.application.analysis.facade

import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.analysis.service.AiBudgetService
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.prompt.service.PromptService
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.share.service.ShareService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * 대기 중인 분석 작업을 하나씩 꺼내 AI 점수를 매기고 순위를 다시 계산하는 백그라운드 작업자.
 *
 * 리스 획득, AI 호출, 결과 기록을 한 트랜잭션으로 묶으면 느린 AI 호출 동안 SQLite 쓰기 잠금을 쥐게 된다.
 * 그래서 이 facade만 [TransactionTemplate]으로 리스 획득, 프롬프트 읽기, 재사용 확인, 사용량 기록, 결과 기록 트랜잭션을 따로 나눈다.
 *
 * AI를 부를 때마다 채점 프롬프트(`score`)의 최신 판을 DB에서 읽는다. 캐시하지 않으므로 관리자 API로 저장한 새 판이
 * 다음 작업부터 바로 쓰인다. 프롬프트가 하나도 없으면 작업은 ANALYSIS_FAILED로 실패하고 로그에 PROMPT_NOT_FOUND가 남는다.
 *
 * 같은 참가자가 같은 채점 입력(모델, 프롬프트 판, AI 사용자 메시지)으로 다시 내면 AI를 부르지 않고 지난 결과를 다시 쓴다.
 * 앱 안의 비용 가드([AiBudgetService])가 하루·한 달 비용 한도에 닿았거나 제공자 문제로 멈춘 동안에는 작업을 잡지 않는다.
 * 로그에는 프롬프트 판 번호와 토큰 수 같은 숫자만 남긴다. 프롬프트, 제출 메시지, 모델 응답은 남기지 않는다.
 */
@Facade
class AnalysisWorkerFacade(
    private val analysisJobService: AnalysisJobService,
    private val analysisScoringService: AnalysisScoringService,
    private val assessmentService: AssessmentService,
    private val aiBudgetService: AiBudgetService,
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
        if (!analysisScoringService.isEnabled() || isAiBlocked()) return
        val lease = UUID.randomUUID().toString()
        val job = transactionTemplate.execute { claimNextJob(lease) } ?: return
        try {
            scoreAndComplete(job, lease)
        } catch (exception: Exception) {
            // 제공자 응답 본문이나 제출 메시지는 로그에 남기지 않는다. 실패한 작업은 새 요청으로만 다시 시도한다.
            logger.warn("분석 작업 {} 실패: {}", job.id, failureReason(exception))
            handleFailure(job.id, lease, exception)
        }
    }

    /** 비용 한도에 닿았거나 제공자 문제로 멈춘 동안이면 참. 작업은 대기열에 남고 오래 밀리면 만료가 정리한다. */
    private fun isAiBlocked(): Boolean = transactionTemplate.execute { aiBudgetService.isBlocked(clock.instant()) } ?: false

    /** 채점 입력이 지난번과 같으면 지난 결과를 다시 쓰고, 아니면 AI 점수를 받아 리스를 아직 쥐고 있을 때 결과를 기록한다. */
    private fun scoreAndComplete(job: AnalysisJob, lease: String) {
        val prompt = latestScorePrompt()
        logger.info("분석 작업 {} 채점: 프롬프트 {} {}판", job.id, prompt.name, prompt.version)
        val digest = analysisScoringService.digest(job)
        val digestHash = analysisScoringService.fingerprint(digest, prompt.version)
        if (transactionTemplate.execute { reuseIfSameDigest(job.id, lease, digestHash) } == true) return
        val score = askAndScore(job, lease, prompt, digest, digestHash)
        transactionTemplate.executeWithoutResult { complete(job.id, lease, score, prompt.version, digestHash) }
        aiBudgetService.recordSuccess()
    }

    /**
     * AI에 묻고 사용량을 기록한 뒤 답을 점수로 바꾼다. 입력에 따라 정해지는 실패(근거 부족, 응답 오류)는
     * 입력 해시와 함께 바로 기록해 같은 입력의 재제출이 AI를 다시 부르지 않게 한다.
     */
    private fun askAndScore(job: AnalysisJob, lease: String, prompt: Prompt, digest: ScoringDigest, digestHash: String): Score {
        val answer = analysisScoringService.ask(digest, prompt.content)
        transactionTemplate.executeWithoutResult { aiBudgetService.record(answer.usage, clock.instant()) }
        try {
            return answer.toScore(digest.lineCount()).also { logUsage(job, prompt, digest, answer, SCORED_OUTCOME) }
        } catch (exception: SantaCutException) {
            logUsage(job, prompt, digest, answer, exception.errorCode.name)
            if (AnalysisFailureCode.isReusableScoringFailure(exception)) {
                transactionTemplate.executeWithoutResult { finishIfHeld(job.id, lease, ScoringFailure.of(exception), digestHash) }
            }
            throw exception
        }
    }

    /** 호출 한 번의 숫자만 남긴다. 근거 보정은 범위 밖·겹친 근거 번호를 지운 수다. 대화문 줄 수와 길이 한도로 뺀 줄 수도 남긴다. */
    private fun logUsage(job: AnalysisJob, prompt: Prompt, digest: ScoringDigest, answer: ScoreAnswer, outcome: String) {
        val usage = answer.usage
        logger.info(
            "분석 작업 {} AI 사용량: 프롬프트 {}판, 메시지 {}개, 대화문 {}줄 {}자(뺀 줄 {}), 입력 {} 출력 {} 생각 {} 토큰, 종료 {}, 근거 보정 {}개, 결과 {}",
            job.id, prompt.version, digest.messageCount, digest.lineCount(), digest.turns.totalLength(), digest.omittedTurns,
            usage.inputTokens, usage.outputTokens, usage.thoughtTokens, usage.finishReason, answer.discardedEvidence(digest.lineCount()), outcome,
        )
    }

    /** 채점 프롬프트의 최신 판을 AI 호출 전에 짧은 트랜잭션에서 읽는다. 없으면 PROMPT_NOT_FOUND다. */
    private fun latestScorePrompt(): Prompt =
        transactionTemplate.execute { promptService.latest(PromptName.SCORE) }
            ?: throw SantaCutException(ErrorCode.PROMPT_NOT_FOUND, PromptName.SCORE.value)

    /** 꺼낼 수 있는 다음 작업의 리스를 잡는다. */
    private fun claimNextJob(lease: String): AnalysisJob? {
        val now = clock.instant()
        val job = analysisJobService.findClaimable(now) ?: return null
        return analysisJobService.startAttempt(job, lease, now)
    }

    /**
     * 같은 참가자가 같은 채점 입력으로 낸 지난 작업이 있으면 그 결과로 이 작업을 끝내고 참을 돌려준다.
     * 리스를 잃었거나 시즌이 마감됐어도 더 할 일이 없으니 참이다.
     */
    private fun reuseIfSameDigest(jobId: String, lease: String, digestHash: String): Boolean {
        val job = analysisJobService.findHeld(jobId, lease) ?: return true
        val previous = analysisJobService.findSameDigest(job, digestHash) ?: return false
        logger.info("분석 작업 {} 채점 입력이 지난 작업 {}와 같아 AI를 부르지 않는다", job.id, previous.id)
        val now = clock.instant()
        if (!analysisJobService.freezeIfSeasonClosed(job, now)) finishLike(job, previous, digestHash, now)
        return true
    }

    /** 지난 작업이 성공했으면 그 평가를 옮겨 적고 순위를 다시 계산한다. 실패했으면 같은 이유로 끝낸다. */
    private fun finishLike(job: AnalysisJob, previous: AnalysisJob, digestHash: String, now: Instant) {
        val previousAssessmentId = previous.reusableAssessmentId()
        if (previousAssessmentId == null) {
            analysisJobService.fail(job, previous.reusableFailure() ?: AnalysisFailureCode.ANALYSIS_FAILED, now, digestHash)
            return
        }
        val assessment = assessmentService.reuse(job, previousAssessmentId, now)
        analysisJobService.succeed(job, assessment.id, now, digestHash)
        rankingService.rebuild(now, shareService.findAll())
    }

    /**
     * 평가를 저장하고 같은 트랜잭션에서 순위를 다시 계산한다. 순위 계산이 실패하면 둘 다 롤백된다.
     * 시즌이 이미 마감됐으면 작업 쪽 규칙에 따라 실패로 끝내고 평가를 저장하지 않는다.
     */
    private fun complete(jobId: String, lease: String, score: Score, promptVersion: Int, digestHash: String) {
        val job = analysisJobService.findHeld(jobId, lease) ?: return
        val completedAt = clock.instant()
        if (analysisJobService.freezeIfSeasonClosed(job, completedAt)) return
        val assessment = assessmentService.record(job, score, promptVersion, completedAt)
        analysisJobService.succeed(job, assessment.id, completedAt, digestHash)
        rankingService.rebuild(completedAt, shareService.findAll())
    }

    /** 실패 종류([ScoringFailure])에 따라 작업을 끝내거나 대기열로 되돌리고, 작업자를 멈출지는 비용 가드가 정한다. */
    private fun handleFailure(jobId: String, lease: String, exception: Exception) {
        val failure = ScoringFailure.of(exception)
        transactionTemplate.executeWithoutResult { finishIfHeld(jobId, lease, failure, null) }
        aiBudgetService.recordFailure(failure, clock.instant())
    }

    /** 리스를 아직 쥐고 있으면 실패 이유로 작업을 끝낸다. 실패 이유가 없는 종류(제공자 차단)는 대기열로 되돌린다. */
    private fun finishIfHeld(jobId: String, lease: String, failure: ScoringFailure, digestHash: String?) {
        val job = analysisJobService.findHeld(jobId, lease) ?: return
        val failureCode = failure.failureCode ?: return analysisJobService.releaseLease(job)
        analysisJobService.fail(job, failureCode, clock.instant(), digestHash)
    }

    private fun failureReason(exception: Exception): String =
        if (exception is SantaCutException) exception.logReason() else exception.javaClass.simpleName

    companion object {
        /** `santacut.ai.poll-delay-ms`가 없을 때 작업자가 다음 작업을 찾기 전 쉬는 시간(밀리초). */
        private const val DEFAULT_POLL_DELAY_MS = 3000L
        private const val SCORED_OUTCOME = "SCORED"
    }
}
