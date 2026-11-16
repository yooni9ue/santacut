package com.santacut.infrastructure.analysis.repository

import com.santacut.domain.analysis.model.AiUsageDay
import org.springframework.data.jpa.repository.JpaRepository

interface AiUsageDayRepository : JpaRepository<AiUsageDay, String> {
    /** `yyyy-MM-dd` 문자열은 사전순과 날짜순이 같아 범위로 찾을 수 있다. */
    fun findAllByUsageDateBetween(from: String, to: String): List<AiUsageDay>
}
