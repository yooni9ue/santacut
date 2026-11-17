package com.santacut.application.ranking.facade

import com.santacut.application.global.Facade
import com.santacut.application.global.TransactionRunner
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.application.ranking.result.MyRankingResult
import com.santacut.application.ranking.result.RankingBoardResult
import com.santacut.domain.ranking.model.RankingCursor
import com.santacut.domain.ranking.service.RankingService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Facade
class ResultFacade(
    private val rankingService: RankingService,
    private val clock: Clock,
    private val transaction: TransactionRunner,
) {
    private val logger = LoggerFactory.getLogger(ResultFacade::class.java)

    @Transactional(readOnly = true)
    fun getMyResult(command: MyResultCommand): MyRankingResult {
        val result = rankingService.findLatestResult(command.participantId)
        val profile = rankingService.findProfile(result.assessment, result.snapshot)
        return MyRankingResult.from(result, profile, rankingService.stageAt(clock.instant()))
    }

    /**
     * 앱이 뜰 때 장난기 기준값과 유형 분포를 다시 센다. 결과 화면이 요청마다 평가 전체를 세지 않게 한다.
     * 다시 세기에 실패해도 앱 시작을 막지 않고 로그만 남긴다. 결과 화면은 지난번 저장한 분포(없으면 빈 분포)로 읽는다.
     * 실패한 트랜잭션을 여기서 롤백하고 예외를 삼켜야 하므로 `@Transactional` 대신 [TransactionRunner]를 쓴다.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun refreshPopulation() {
        try {
            transaction.run { rankingService.refreshPopulation() }
        } catch (exception: RuntimeException) {
            logger.warn("장난기 기준값과 유형 분포를 다시 세지 못했다. 저장한 분포로 결과를 보여 준다", exception)
        }
    }

    @Transactional(readOnly = true)
    fun getRankings(command: RankingPageCommand): RankingBoardResult = RankingBoardResult.from(
        rankingService.findRankingBoard(RankingCursor.from(command.cursor), clock.instant()),
    )
}
