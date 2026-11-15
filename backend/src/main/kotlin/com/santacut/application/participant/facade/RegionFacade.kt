package com.santacut.application.participant.facade

import com.santacut.application.global.Facade
import com.santacut.application.participant.result.RegionResults
import com.santacut.domain.participant.service.RegionService
import org.springframework.transaction.annotation.Transactional

@Facade
class RegionFacade(
    private val regionService: RegionService,
) {
    @Transactional(readOnly = true)
    fun listRegions(): RegionResults = RegionResults.from(regionService.findAll())
}
