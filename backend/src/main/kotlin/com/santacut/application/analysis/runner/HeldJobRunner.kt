package com.santacut.application.analysis.runner

import com.santacut.application.global.Runner
import com.santacut.application.global.TransactionRunner
import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.model.ScoringAttempt
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.dossier.model.DossierDraft
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.roast.model.RoastDraft
import com.santacut.domain.roast.service.RoastService
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 채점 작업을 리스로 잡고, 리스를 아직 쥔 동안에만 결과를 기록하는 실행기.
 *
 * 리스 획득, AI 호출, 결과 기록을 한 트랜잭션으로 묶으면 느린 AI 호출 동안 SQLite 쓰기 잠금을 쥐게 된다.
 * 그래서 만료 정리, 리스 획득, 재사용 확인, 결과 기록, 실패 정리를 각각 짧은 트랜잭션으로 나눈다.
 * 작업 잡기는 커밋까지 [claimLock] 안에서 해 두 스레드가 같은 작업을 잡지 않게 한다.
 * 리스 토큰은 잡은 작업([AnalysisJob.leaseToken])에 있다. 리스를 잃었으면(다른 작업자가 가져갔거나 이미 끝났으면) 아무것도 쓰지 않는다.
 */
@Runner
class HeldJobRunner(
    private val analysisJobService: AnalysisJobService,
    private val assessmentService: AssessmentService,
    private val dossierService: DossierService,
    private val roastService: RoastService,
    private val rankingService: RankingService,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(HeldJobRunner::class.java)
    private val claimLock = ReentrantLock()

    /**
     * AI가 꺼져 있거나 비용 한도에 막혀도 리스가 끝난 실행 작업을 대기열로 되돌리고 24시간 넘게 기다린 작업은 정리한다(기획 §4.6).
     * 리스 획득과 따로 짧은 트랜잭션으로 연다.
     */
    fun expireOverdue() = transaction.run { analysisJobService.expireOverdueJobs(clock.instant()) }

    /**
     * 꺼낼 수 있는 다음 작업의 리스를 잡는다. 시즌이 마감됐으면 그 자리에서 AI 없이 끝낸다.
     * 찾기와 리스 기록을 한 트랜잭션에 두고 커밋까지 [claimLock]으로 묶어, 다른 스레드가 같은 작업을 동시에 찾지 못하게 한다.
     */
    fun claimNext(): AnalysisJob? {
        val lease = UUID.randomUUID().toString()
        return claimLock.withLock { transaction.call { analysisJobService.claimNext(lease, clock.instant()) } }
    }

    /**
     * 저장할 조서를 트랜잭션 밖에서 미리 만든다. 집계와 JSON 직렬화는 CPU만 쓰므로 쓰기 잠금을 쥐기 전에 하고,
     * 작업 성공이 제출 내용을 지우므로 그 전에 한다. 집계에 실패하면 null이고 조서 없이 결과를 저장한다.
     */
    fun draftDossier(job: AnalysisJob): DossierDraft? = dossierService.draft(job)

    /**
     * 같은 참가자가 같은 채점 입력으로 낸 지난 작업이 있으면 그 결과와 조서([dossier]는 지난 조서가 없을 때 쓴다)로
     * 이 작업을 끝내고 참을 돌려준다. 리스를 잃었거나 시즌이 마감됐어도 더 할 일이 없으니 참이다.
     */
    fun reuseIfSameDigest(attempt: ScoringAttempt, dossier: DossierDraft?, roast: RoastDraft? = null): Boolean =
        transaction.call { reuseHeld(attempt, dossier, roast) } == true

    /**
     * 같은 채점 입력으로 성공한 지난 작업이 있는데 그 팩폭 결과를 그대로 옮길 수 없으면([RoastService.isReusable]) 지난 평가.
     * 그 밖에는 null이다. 시즌이 이미 마감됐으면 재사용이 SEASON_FROZEN으로 끝나므로 작가를 부르지 않게 null이다.
     * 호출하는 쪽은 이 평가의 총점과 항목 점수로 트랜잭션 밖에서 작가를 한 번 부르고 [reuseIfSameDigest]에 넘긴다.
     */
    fun assessmentNeedingRoast(attempt: ScoringAttempt): Assessment? = transaction.call {
        if (analysisJobService.isSeasonClosedAt(clock.instant())) return@call null
        val job = analysisJobService.findHeld(attempt.job) ?: return@call null
        val previousAssessmentId = analysisJobService.findSameDigest(job, attempt.digestHash)?.reusableAssessmentId() ?: return@call null
        if (roastService.isReusable(previousAssessmentId)) null else assessmentService.get(previousAssessmentId)
    }

    /**
     * 평가와 미리 만든 조서([dossier]), 팩폭 결과([roast])를 저장하고 같은 트랜잭션에서 순위 다시 계산을 요청한다.
     * 계산은 순위 다시 계산 작업자가 모아서 한다. 시즌이 이미 마감됐으면 작업 쪽 규칙에 따라 실패로 끝내고 평가를 저장하지 않는다.
     */
    fun complete(attempt: ScoringAttempt, scored: ScoredAssessment, dossier: DossierDraft?, roast: RoastDraft) {
        transaction.run {
            val job = analysisJobService.findHeld(attempt.job) ?: return@run
            val completedAt = clock.instant()
            if (analysisJobService.freezeIfSeasonClosed(job, completedAt)) return@run
            val assessment = assessmentService.record(job, scored, completedAt)
            dossierService.record(assessment, dossier, completedAt)
            roastService.record(assessment, roast, completedAt)
            analysisJobService.succeed(job, assessment.id, completedAt, attempt.digestHash)
            rankingService.requestRebuild()
        }
    }

    /**
     * 리스를 아직 쥐고 있으면 실패 종류([ScoringFailure])에 따라 작업을 끝내거나 대기열로 되돌린다([AnalysisJob.settle]).
     * 받은 답에서 난 실패만 채점 입력 해시([digestHash])와 과금 여부([billed])를 넘긴다.
     */
    fun settle(job: AnalysisJob, failure: ScoringFailure, digestHash: String? = null, billed: Boolean = false) =
        transaction.run { analysisJobService.settleFailure(job, failure, digestHash, billed, clock.instant()) }

    private fun reuseHeld(attempt: ScoringAttempt, dossier: DossierDraft?, roast: RoastDraft?): Boolean {
        val job = analysisJobService.findHeld(attempt.job) ?: return true
        val previous = analysisJobService.findSameDigest(job, attempt.digestHash) ?: return false
        logger.info("분석 작업 {} 채점 입력이 지난 작업 {}와 같아 AI를 부르지 않는다", job.id, previous.id)
        val now = clock.instant()
        if (!analysisJobService.freezeIfSeasonClosed(job, now)) finishLike(job, previous, attempt.digestHash, dossier, roast, now)
        return true
    }

    /**
     * 지난 작업이 성공했으면 그 평가와 조서 집계, 팩폭 결과를 옮겨 적고 순위 다시 계산을 요청한다. 실패했으면 같은 이유로 끝낸다.
     * 새로 쓴 팩폭 결과 [roast]가 있으면(지난 결과를 그대로 옮길 수 없을 때) 그것을, 없으면 지난 결과를 옮겨 적는다.
     */
    private fun finishLike(job: AnalysisJob, previous: AnalysisJob, digestHash: String, dossier: DossierDraft?, roast: RoastDraft?, now: Instant) {
        val previousAssessmentId = previous.reusableAssessmentId()
        if (previousAssessmentId == null) {
            analysisJobService.fail(job, previous.reusableFailure() ?: AnalysisFailureCode.ANALYSIS_FAILED, now, digestHash)
            return
        }
        val assessment = assessmentService.reuse(job, previousAssessmentId, now)
        dossierService.reuse(previousAssessmentId, assessment, dossier, now)
        if (roast != null) roastService.record(assessment, roast, now) else roastService.reuse(previousAssessmentId, assessment, dossier, now)
        analysisJobService.succeed(job, assessment.id, now, digestHash)
        rankingService.requestRebuild()
    }
}
