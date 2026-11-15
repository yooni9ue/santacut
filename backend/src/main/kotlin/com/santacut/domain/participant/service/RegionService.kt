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

    fun verifySupported(regionCode: String) {
        if (!regionRepository.existsById(regionCode)) throw SantaCutException(ErrorCode.UNSUPPORTED_REGION)
    }
}
