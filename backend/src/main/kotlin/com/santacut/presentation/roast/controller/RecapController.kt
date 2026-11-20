package com.santacut.presentation.roast.controller

import com.santacut.application.roast.command.MyRecapCommand
import com.santacut.application.roast.facade.RecapFacade
import com.santacut.presentation.roast.docs.GetMyRecapApi
import com.santacut.presentation.roast.response.RecapResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RecapController(
    private val recapFacade: RecapFacade,
) {
    @GetMapping("/v1/me/recap")
    @GetMyRecapApi
    fun myRecap(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): RecapResponse =
        RecapResponse.from(recapFacade.getMyRecap(MyRecapCommand(participantId)))
}
