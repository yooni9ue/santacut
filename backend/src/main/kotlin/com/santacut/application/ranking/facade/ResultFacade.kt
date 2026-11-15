package com.santacut.application.ranking.facade

import com.santacut.application.global.Facade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.application.ranking.result.MyRankingResult
import com.santacut.application.ranking.result.RankingRowResults
import com.santacut.domain.ranking.model.RankingPage
import com.santacut.domain.ranking.service.RankingService
import org.springframework.transaction.annotation.Transactional

@Facade
class ResultFacade(
    private val rankingService: RankingService,
) {
    @Transactional(readOnly = true)
    fun getMyResult(command: MyResultCommand): MyRankingResult =
        MyRankingResult.from(rankingService.findLatestResult(command.participantId))

    @Transactional(readOnly = true)
    fun getRankings(command: RankingPageCommand): RankingRowResults =
        RankingRowResults.from(rankingService.findRankingPage(RankingPage.of(command.offset)))
}
