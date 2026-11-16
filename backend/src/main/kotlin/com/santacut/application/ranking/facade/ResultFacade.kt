package com.santacut.application.ranking.facade

import com.santacut.application.global.Facade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.application.ranking.result.MyRankingResult
import com.santacut.application.ranking.result.RankingBoardResult
import com.santacut.domain.ranking.model.RankingCursor
import com.santacut.domain.ranking.service.RankingService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Facade
class ResultFacade(
    private val rankingService: RankingService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getMyResult(command: MyResultCommand): MyRankingResult {
        val result = rankingService.findLatestResult(command.participantId)
        val profile = rankingService.findProfile(result.assessment, result.snapshot)
        return MyRankingResult.from(result, profile, rankingService.stageAt(clock.instant()))
    }

    @Transactional(readOnly = true)
    fun getRankings(command: RankingPageCommand): RankingBoardResult = RankingBoardResult.from(
        rankingService.findRankingBoard(RankingCursor.from(command.cursor), clock.instant()),
    )
}
