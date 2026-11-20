package com.santacut.application.ranking.facade

import com.santacut.application.global.Facade
import com.santacut.application.global.TransactionRunner
import com.santacut.application.ranking.runner.RankingRebuildDebounceRunner
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.ranking.model.RankingRebuildRequested
import com.santacut.domain.ranking.service.RankingService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 쌓인 순위 다시 계산 요청을 모아 한 번에 처리한다(backend-design.md §7.5).
 *
 * 심사 성공, 지역 변경, 참가자 삭제는 요청 표시만 남긴다. 그 트랜잭션이 커밋되면 이 facade가 기다린 뒤 그 사이 쌓인 요청을
 * 한 번에 다시 계산한다([RankingRebuildDebounceRunner]). 기다리는 시간은 모으기 시간(기본 1초)과, 지난 계산이 끝난 뒤
 * 최소 간격(기본 5초)까지 남은 시간 가운데 긴 쪽이다. 그래서 한가할 때는 결과가 커밋 직후 순위에 오르고, 작업이 몰려도 계산은
 * 최소 간격마다 한 번을 넘지 않는다. 계산마다 참가자 수만큼 스냅숏을 새로 쓰므로 이 상한이 시즌 동안 쓰는 행 수를 묶는다.
 * 일정한 간격의 예약 작업은 커밋 뒤 예약을 놓쳤을 때(앱 재시작, 계산 실패)의 안전망으로 남긴다.
 * 시즌 마감 시각(12월 25일 0시, Asia/Seoul)에 한 번 더 돌려 마감 직전 변경까지 확정 순위에 넣고, 앱이 뜰 때도 남은 요청을 처리한다.
 * 다시 계산한 뒤에는 그 사이 성공한 작업을 다시 알려, 진행 상황 스트림이 RANKING_PENDING 다음에 SUCCEEDED를 보내게 한다.
 *
 * 커밋 뒤 계산, 예약 작업, 마감 작업, 앱 시작이 겹치지 않도록 [rebuildLock]으로 하나씩만 돌린다.
 * 실패해도 요청 표시가 남아 다음 차례에 다시 시도하도록, 트랜잭션을 롤백한 뒤 경계 밖에서 예외를 잡아야 해서 [TransactionRunner]를 쓴다.
 */
@Facade
class RankingRebuildFacade(
    private val rankingService: RankingService,
    private val analysisJobService: AnalysisJobService,
    private val transaction: TransactionRunner,
    private val debounce: RankingRebuildDebounceRunner,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(RankingRebuildFacade::class.java)
    private val rebuildLock = ReentrantLock()

    /** 요청이 쌓여 있으면 순위를 다시 계산한다. 커밋 뒤 예약을 놓친 요청을 처리하는 안전망이라 남은 요청이 없으면 표시 행 하나만 읽는다. */
    @Scheduled(
        fixedDelayString = "\${santacut.ranking.rebuild-delay-ms:$DEFAULT_REBUILD_DELAY_MS}",
        initialDelayString = "\${santacut.ranking.rebuild-delay-ms:$DEFAULT_REBUILD_DELAY_MS}",
    )
    fun rebuildIfRequested() = oneAtATime { rebuildAndAnnounce() }

    /**
     * 순위 다시 계산을 요청한 트랜잭션이 커밋되면 곧 한 번 다시 계산하도록 예약한다. 다른 스레드에서 돌고 실패해도 로그만 남기므로
     * 요청한 쪽(작업 성공 기록 등)의 결과에 영향을 주지 않는다. 실패하면 요청 표시가 남아 다음 차례에 다시 시도한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun rebuildSoon(requested: RankingRebuildRequested) = debounce.rebuildSoon(::runSafely)

    /** 시즌 마감 순간 남은 요청을 확정 순위에 넣는다. 시즌 연도와 상관없이 해마다 돌지만 요청이 없으면 아무것도 하지 않는다. */
    @Scheduled(cron = SEASON_FREEZE_CRON, zone = SEOUL_ZONE)
    fun rebuildAtSeasonFreeze() {
        logger.info("시즌 마감 시각이라 남은 순위 계산 요청을 처리한다")
        runSafely()
    }

    /** 앱이 뜰 때 남은 요청을 처리한다. 실패해도 앱 시작을 막지 않는다. */
    @EventListener(ApplicationReadyEvent::class)
    fun rebuildOnStartup() {
        runSafely()
    }

    private fun runSafely() {
        try {
            rebuildIfRequested()
        } catch (exception: Exception) {
            logger.warn("순위를 다시 계산하지 못했다. 요청 표시가 남아 다음 차례에 다시 시도한다", exception)
        }
    }

    /**
     * 한 번에 하나만, 트랜잭션 하나로 [step]을 돌린다. 실패하면 롤백하고 예외를 다시 던진다.
     * 잠금을 잡자마자 커밋 뒤 예약 표시를 내리고, 끝나면(실패해도) 최소 간격을 잴 시각을 남긴다.
     */
    private fun oneAtATime(step: () -> Unit) = rebuildLock.withLock {
        debounce.rebuildStarted()
        try {
            transaction.run(step)
        } finally {
            debounce.rebuildFinished()
        }
    }

    private fun rebuildAndAnnounce() {
        val startedAt = System.nanoTime()
        val previousComputedAt = rankingService.latestComputedAt()
        if (!rankingService.rebuildIfRequested(clock.instant())) return
        analysisJobService.announceRankedSince(previousComputedAt)
        logger.info("순위를 다시 계산했다: {}ms", Duration.ofNanos(System.nanoTime() - startedAt).toMillis())
    }

    companion object {
        private const val DEFAULT_REBUILD_DELAY_MS = 30_000L
        private const val SEASON_FREEZE_CRON = "0 0 0 25 12 *"
        private const val SEOUL_ZONE = "Asia/Seoul"
    }
}
