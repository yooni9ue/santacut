package com.santacut.domain.service

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import com.santacut.domain.model.Regions
import com.santacut.infrastructure.repository.RegionRepository
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
