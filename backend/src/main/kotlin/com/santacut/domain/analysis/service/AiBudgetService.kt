package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AiBudget
import com.santacut.domain.analysis.model.AiUsageDay
import com.santacut.domain.analysis.model.AiUsageDays
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ProviderBackoff
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.global.config.AiBudgetProperties
import com.santacut.domain.global.model.SeoulTime
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

/**
 * 앱 안의 AI 비용 가드. 호출마다 토큰 사용량을 날짜별로 더하고, 하루·한 달 비용이 한도에 닿거나
 * 제공자 문제로 멈춘 동안에는 작업을 잡지 못하게 한다. 날짜와 달은 한국 시각 기준이다.
 */
@Service
class AiBudgetService(
    private val aiUsageDayRepository: AiUsageDayRepository,
    aiBudgetProperties: AiBudgetProperties,
) {
    private val budget = AiBudget(
        aiBudgetProperties.dailyUsd,
        aiBudgetProperties.monthlyUsd,
        aiBudgetProperties.inputUsdPerMillionTokens,
        aiBudgetProperties.outputUsdPerMillionTokens,
    )
    private val backoff = ProviderBackoff(aiBudgetProperties.pause, aiBudgetProperties.maxConsecutiveFailures)

    /** 잠시 멈춘 동안이거나 비용 한도에 닿았으면 참. 이때 작업은 대기열에 그대로 남는다. */
    fun isBlocked(now: Instant): Boolean {
        if (backoff.isPausedAt(now)) return true
        val today = LocalDate.ofInstant(now, SeoulTime.ZONE)
        return budget.isExhausted(usageBetween(today, today), usageBetween(today.withDayOfMonth(1), today))
    }

    /** 호출 한 번의 사용량을 오늘 합계에 더한다. 사용량을 받지 못한 호출([ModelUsage.NONE])은 더하지 않는다. */
    fun record(usage: ModelUsage, now: Instant) {
        if (usage == ModelUsage.NONE) return
        val key = AiUsageDay.keyOf(LocalDate.ofInstant(now, SeoulTime.ZONE))
        val day = aiUsageDayRepository.findById(key).orElse(null) ?: AiUsageDay(key)
        day.add(usage)
        aiUsageDayRepository.save(day)
    }

    /** 실패 종류에 따라 바로 멈추거나 연속 실패로 센다. 규칙은 [ProviderBackoff]가 정한다. */
    fun recordFailure(failure: ScoringFailure, now: Instant) = backoff.record(failure, now)

    fun recordSuccess() = backoff.recordSuccess()

    private fun usageBetween(from: LocalDate, to: LocalDate): AiUsageDays =
        AiUsageDays(aiUsageDayRepository.findAllByUsageDateBetween(AiUsageDay.keyOf(from), AiUsageDay.keyOf(to)))
}
