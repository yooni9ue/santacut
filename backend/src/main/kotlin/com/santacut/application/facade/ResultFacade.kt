package com.santacut.application.facade

import com.santacut.application.command.MyResultCommand
import com.santacut.application.command.RankingPageCommand
import com.santacut.application.result.MyRankingResult
import com.santacut.application.result.RankingRowResults
import com.santacut.domain.model.RankingPage
import com.santacut.domain.service.RankingService
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
