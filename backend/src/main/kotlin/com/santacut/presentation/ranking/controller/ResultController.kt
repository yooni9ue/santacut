package com.santacut.presentation.ranking.controller

import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.application.ranking.facade.ResultFacade
import com.santacut.presentation.ranking.docs.GetMyResultApi
import com.santacut.presentation.ranking.docs.GetRankingsApi
import com.santacut.presentation.ranking.response.MyResultResponse
import com.santacut.presentation.ranking.response.RankingBoardResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ResultController(
    private val resultFacade: ResultFacade,
) {
    @GetMapping("/v1/me/result")
    @GetMyResultApi
    fun myResult(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): MyResultResponse =
        MyResultResponse.from(resultFacade.getMyResult(MyResultCommand(participantId)))

    @GetMapping("/v1/rankings")
    @GetRankingsApi
    fun rankings(@RequestParam(required = false) cursor: String?): RankingBoardResponse =
        RankingBoardResponse.from(resultFacade.getRankings(RankingPageCommand(cursor)))
}
