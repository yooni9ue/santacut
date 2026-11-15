package com.santacut.presentation.controller

import com.santacut.application.facade.RegionFacade
import com.santacut.presentation.docs.ListRegionsApi
import com.santacut.presentation.response.RegionResponses
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RegionController(
    private val regionFacade: RegionFacade,
) {
    @GetMapping("/v1/regions")
    @ListRegionsApi
    fun regions(): RegionResponses = RegionResponses.from(regionFacade.listRegions())
}
