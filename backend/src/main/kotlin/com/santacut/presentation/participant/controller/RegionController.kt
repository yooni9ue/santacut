package com.santacut.presentation.participant.controller

import com.santacut.application.participant.command.ListRegionsCommand
import com.santacut.application.participant.facade.RegionFacade
import com.santacut.presentation.participant.docs.ListRegionsApi
import com.santacut.presentation.participant.response.RegionResponses
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RegionController(
    private val regionFacade: RegionFacade,
) {
    @GetMapping("/v1/regions")
    @ListRegionsApi
    fun regions(@RequestParam(required = false) parent: String?): RegionResponses =
        RegionResponses.from(regionFacade.listRegions(ListRegionsCommand(parent)))
}
