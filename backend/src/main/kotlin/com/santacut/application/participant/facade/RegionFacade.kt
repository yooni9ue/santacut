package com.santacut.application.participant.facade

import com.santacut.application.global.Facade
import com.santacut.application.participant.command.ListRegionsCommand
import com.santacut.application.participant.result.RegionResults
import com.santacut.domain.participant.service.RegionService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional

@Facade
class RegionFacade(
    private val regionService: RegionService,
) {
    @Transactional(readOnly = true)
    fun listRegions(command: ListRegionsCommand): RegionResults =
        RegionResults.childrenOf(regionService.findAll(), command.parentCode)

    /**
     * 앱이 뜰 때 지역 계층을 잇는다. 시드 SQL은 지역 행만 넣고 계층은
     * 코드 접두어로 여기서 잇는다. 이미 이어진 지역은 건드리지 않는다.
     */
    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun linkRegionHierarchy() = regionService.linkHierarchy()
}
