package com.santacut.domain.participant.service

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.participant.model.Regions
import com.santacut.infrastructure.participant.repository.RegionRepository
import org.springframework.stereotype.Service

@Service
class RegionService(
    private val regionRepository: RegionRepository,
) {
    fun findAll(): Regions = Regions(regionRepository.findAllByOrderByCodeAsc())

    /** 없는 지역이거나 하위 지역을 골라야 하는 지역(구가 있는 시·도)이면 [ErrorCode.UNSUPPORTED_REGION]이다. */
    fun verifySupported(regionCode: String) {
        if (!findAll().isSelectable(regionCode)) throw SantaCutException(ErrorCode.UNSUPPORTED_REGION)
    }

    /** 상위 지역이 비어 있는 지역을 코드 체계에 따라 잇는다. 여러 번 실행해도 결과가 같다. */
    fun linkHierarchy() {
        regionRepository.linkParentsByCodePrefix()
    }
}
