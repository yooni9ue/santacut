package com.santacut.application.facade

import com.santacut.application.result.RegionResults
import com.santacut.domain.service.RegionService
import org.springframework.transaction.annotation.Transactional

@Facade
class RegionFacade(
    private val regionService: RegionService,
) {
    @Transactional(readOnly = true)
    fun listRegions(): RegionResults = RegionResults.from(regionService.findAll())
}
