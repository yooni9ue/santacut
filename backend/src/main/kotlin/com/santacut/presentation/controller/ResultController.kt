package com.santacut.presentation.controller

import com.santacut.application.command.MyResultCommand
import com.santacut.application.command.RankingPageCommand
import com.santacut.application.facade.ResultFacade
import com.santacut.presentation.docs.GetMyResultApi
import com.santacut.presentation.docs.GetRankingsApi
import com.santacut.presentation.response.MyResultResponse
import com.santacut.presentation.response.RankingResponses
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
    fun rankings(@RequestParam(defaultValue = "0") offset: Int): RankingResponses =
        RankingResponses.from(resultFacade.getRankings(RankingPageCommand(offset)))
}
