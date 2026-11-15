package com.santacut.domain.service

import com.santacut.domain.config.SantaCutProperties
import com.santacut.domain.model.DailyBudget
import com.santacut.infrastructure.repository.DailyBudgetRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AiBudgetService(
    private val dailyBudgetRepository: DailyBudgetRepository,
    private val santaCutProperties: SantaCutProperties,
) {
    /** 오늘(서울) 예산이 남았으면 한 번 쓰고 참을 돌려준다. */
    fun tryConsume(now: Instant): Boolean {
        val day = DailyBudget.dayOf(now)
        val budget = dailyBudgetRepository.findById(day).orElse(null) ?: DailyBudget.startOf(day)
        if (!budget.hasRemaining(santaCutProperties.ai.dailyCallLimit)) return false
        budget.consume()
        dailyBudgetRepository.save(budget)
        return true
    }
}
