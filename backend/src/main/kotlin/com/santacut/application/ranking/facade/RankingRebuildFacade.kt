package com.santacut.application.ranking.facade

import com.santacut.application.global.Facade
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.ranking.service.RankingService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 쌓인 순위 다시 계산 요청을 모아 한 번에 처리한다(backend-design.md §7.5).
 *
 * 심사 성공, 지역 변경, 참가자 삭제는 요청 표시만 남기고, 이 facade가 일정한 간격으로 한 번만 다시 계산한다.
 * 그래서 참가자가 N명일 때 시즌 동안 쓰는 행 수가 변경 횟수 × N으로 늘지 않는다.
 * 시즌 마감 시각(12월 25일 0시, Asia/Seoul)에 한 번 더 돌려 마감 직전 변경까지 확정 순위에 넣고, 앱이 뜰 때도 남은 요청을 처리한다.
 * 다시 계산한 뒤에는 그 사이 성공한 작업을 다시 알려, 진행 상황 스트림이 RANKING_PENDING 다음에 SUCCEEDED를 보내게 한다.
 *
 * 예약 작업, 마감 작업, 앱 시작이 겹치지 않도록 [rebuildLock]으로 하나씩만 돌린다.
 * 실패해도 요청 표시가 남아 다음 차례에 다시 시도하도록, 트랜잭션을 롤백한 뒤 경계 밖에서 예외를 잡아야 해서 [TransactionTemplate]을 쓴다.
 */
@Facade
class RankingRebuildFacade(
    private val rankingService: RankingService,
    private val analysisJobService: AnalysisJobService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(RankingRebuildFacade::class.java)
    private val rebuildLock = ReentrantLock()

    /** 요청이 쌓여 있으면 순위를 다시 계산한다. */
    @Scheduled(
        fixedDelayString = "\${santacut.ranking.rebuild-delay-ms:$DEFAULT_REBUILD_DELAY_MS}",
        initialDelayString = "\${santacut.ranking.rebuild-delay-ms:$DEFAULT_REBUILD_DELAY_MS}",
    )
    fun rebuildIfRequested() {
        rebuildLock.withLock { transactionTemplate.executeWithoutResult { rebuildAndAnnounce() } }
    }

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
        } catch (exception: RuntimeException) {
            logger.warn("순위를 다시 계산하지 못했다. 요청 표시가 남아 다음 차례에 다시 시도한다", exception)
        }
    }

    private fun rebuildAndAnnounce() {
        val previousComputedAt = rankingService.latestComputedAt()
        if (rankingService.rebuildIfRequested(clock.instant())) analysisJobService.announceRankedSince(previousComputedAt)
    }

    companion object {
        private const val DEFAULT_REBUILD_DELAY_MS = 30_000L
        private const val SEASON_FREEZE_CRON = "0 0 0 25 12 *"
        private const val SEOUL_ZONE = "Asia/Seoul"
    }
}
