package com.santacut.application.analysis.facade

import com.santacut.application.analysis.runner.AiBudgetRunner
import com.santacut.application.analysis.runner.AnalysisWorkerRunner
import com.santacut.application.analysis.runner.HeldJobRunner
import com.santacut.application.global.Facade
import com.santacut.application.roast.runner.RoastRunner
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.model.ScoringAttempt
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.analysis.service.AnalysisScoringService
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.prompt.service.PromptService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

/**
 * 대기 중인 분석 작업을 꺼내 AI 점수를 매기고 순위 다시 계산을 요청하는 백그라운드 작업자.
 *
 * 흐름은 `작업 잡기 → 조서 준비 → 같은 입력이면 지난 결과 재사용, 아니면 채점 → 팩폭 쓰기 → 결과 기록`, 실패하면 `정리`다.
 * 채점 스레드와 자리는 [AnalysisWorkerRunner], 리스와 트랜잭션은 [HeldJobRunner], 비용 가드와 잠금은 [AiBudgetRunner],
 * 채점과 독립된 팩폭 작가 호출(실패해도 대체 문장)은 [RoastRunner]가 맡는다.
 *
 * AI를 부를 때마다 채점 프롬프트(`score`)의 최신 판을 DB에서 읽는다. 캐시하지 않으므로 관리자 API로 저장한 새 판이
 * 다음 작업부터 바로 쓰인다. 프롬프트가 하나도 없으면 작업은 ANALYSIS_FAILED로 실패하고 로그에 PROMPT_NOT_FOUND가 남는다.
 * 비용 한도에 닿았거나 제공자 문제로 멈춘 동안에는 작업을 잡지 않는다.
 * 로그에는 프롬프트 판 번호와 토큰 수 같은 숫자만 남긴다. 프롬프트, 제출 메시지, 모델 응답은 남기지 않는다.
 */
@Facade
class AnalysisWorkerFacade(
    private val scoring: AnalysisScoringService,
    private val promptService: PromptService,
    private val workers: AnalysisWorkerRunner,
    private val heldJobs: HeldJobRunner,
    private val budget: AiBudgetRunner,
    private val roasts: RoastRunner,
) {
    private val logger = LoggerFactory.getLogger(AnalysisWorkerFacade::class.java)

    /** 오래 기다린 작업을 정리하고, 빈 채점 스레드마다 작업을 하나씩 잡아 넘긴다. */
    @Scheduled(fixedDelayString = "\${santacut.ai.poll-delay-ms:${DEFAULT_POLL_DELAY_MS}}")
    fun dispatchJobs() {
        heldJobs.expireOverdue()
        workers.dispatch(::claimNext, ::process)
    }

    /** 작업 하나를 이 스레드에서 바로 잡아 채점한다. 잡을 작업이 없으면 아무것도 하지 않는다. */
    fun processNextJob() {
        heldJobs.expireOverdue()
        process(claimNext() ?: return)
    }

    /** AI를 쓸 수 있고 비용 가드가 막지 않을 때만 다음 작업을 잡는다. */
    private fun claimNext(): AnalysisJob? =
        if (scoring.isEnabled() && !budget.isBlocked()) heldJobs.claimNext() else null

    /**
     * 작업 하나를 채점한다. 실패하면 실패 종류에 따라 작업을 정리한다.
     * 스택 넘침([StackOverflowError])은 입력이 정한 실패라 그 작업만 ANALYSIS_FAILED로 끝낸다. 잡지 않으면 리스가 끝날 때마다
     * 같은 작업을 다시 잡아 시도를 다 쓸 때까지 되풀이한다.
     * 메모리 부족([OutOfMemoryError])은 잡지 않는다. JVM 옵션(ExitOnOutOfMemoryError)이 프로세스를 내리고 다시 띄우게 둔다.
     * 제공자 응답 본문이나 제출 메시지는 로그에 남기지 않는다. 실패한 작업은 새 요청으로만 다시 시도한다.
     */
    private fun process(job: AnalysisJob) {
        try {
            reuseOrScore(job)
        } catch (exception: Exception) {
            settle(job, ScoringFailure.of(exception), (exception as? SantaCutException)?.logReason() ?: exception.javaClass.simpleName)
        } catch (error: StackOverflowError) {
            settle(job, ScoringFailure.INPUT_REJECTED, error.javaClass.simpleName)
        }
    }

    /**
     * 조서를 미리 만들어 두고, 채점 입력이 지난번과 같으면 지난 결과를 다시 쓰고, 아니면 AI 점수를 받아 팩폭 문장을 쓴 뒤 결과를 기록한다.
     * 지난 결과를 다시 쓸 때 지난 팩폭 결과를 그대로 옮길 수 없으면(없음, 모두 대체 문장) 트랜잭션 밖에서 작가를 한 번 부른다.
     * 팩폭 쓰기는 실패해도 대체 문장으로 채우므로 채점 결과 기록을 막지 않는다.
     */
    private fun reuseOrScore(job: AnalysisJob) {
        val attempt = scoring.prepare(job, promptService.latest(PromptName.SCORE))
        val dossier = heldJobs.draftDossier(job)
        val freshRoast = heldJobs.scoreNeedingRoast(attempt)?.let { hundredths -> roasts.write(job, hundredths, dossier, budget::spending) }
        if (heldJobs.reuseIfSameDigest(attempt, dossier, freshRoast)) return
        val scored = score(attempt)
        val roast = roasts.write(job, scored.score.hundredths, dossier, budget::spending)
        heldJobs.complete(attempt, scored, dossier, roast)
        budget.recordSuccess()
    }

    /**
     * 어림 비용을 예약한 채 AI에 묻고, 받은 답으로 평가할 결과를 만든다. 받은 답 때문에 실패하면 채점 입력 해시와
     * 과금 여부를 아는 이 자리에서 작업을 먼저 정리한 뒤 예외를 다시 던진다.
     */
    private fun score(attempt: ScoringAttempt): ScoredAssessment {
        val answers = budget.spending(scoring.estimateUsage(attempt)) { record -> scoring.ask(attempt, record) }
        try {
            return scoring.assess(attempt, answers)
        } catch (exception: SantaCutException) {
            heldJobs.settle(attempt.job, ScoringFailure.of(exception), attempt.digestHash, answers.isBilled())
            throw exception
        }
    }

    /** 리스를 아직 쥔 작업을 실패 종류대로 정리하고, 작업자를 멈출지는 비용 가드가 정한다. */
    private fun settle(job: AnalysisJob, failure: ScoringFailure, reason: String) {
        logger.warn("분석 작업 {} 실패: {}", job.id, reason)
        heldJobs.settle(job, failure)
        budget.recordFailure(failure)
    }

    companion object {
        /** `santacut.ai.poll-delay-ms`가 없을 때 작업자가 다음 작업을 찾기 전 쉬는 시간(밀리초). 제출 뒤 채점 시작까지 평균 0.5초를 기다린다. 빈 폴링은 색인 조회 두 번뿐이다. */
        private const val DEFAULT_POLL_DELAY_MS = 1000L
    }
}
