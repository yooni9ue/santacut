package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AiBudget
import com.santacut.domain.analysis.model.AiBudgetPeriod
import com.santacut.domain.analysis.model.AiUsageDay
import com.santacut.domain.analysis.model.AiUsageDays
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ProviderBackoff
import com.santacut.domain.analysis.model.ReservedUsages
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.global.config.AiBudgetProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import org.springframework.stereotype.Service
import java.time.Instant

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
        aiBudgetProperties.cachedInputUsdPerMillionTokens,
        aiBudgetProperties.outputUsdPerMillionTokens,
    )
    private val reservations = ReservedUsages()
    private val backoff = ProviderBackoff(aiBudgetProperties.pause, aiBudgetProperties.maxConsecutiveFailures)

    /** 잠시 멈춘 동안이거나 비용 한도에 닿았으면 참. 이때 작업은 대기열에 그대로 남는다. */
    fun isBlocked(now: Instant): Boolean {
        if (backoff.isPausedAt(now)) return true
        val period = AiBudgetPeriod.at(now)
        return budget.isExhausted(usageBetween(period.dayKey(), period.dayKey()), usageBetween(period.monthStartKey(), period.dayKey()))
    }

    /**
     * 호출 전에 어림 사용량([estimate])을 이미 예약된 다른 작업의 어림 사용량과 함께 더해도 하루·한 달 한도 안인지 확인하고 예약한다.
     * 넘으면 [ErrorCode.AI_BUDGET_EXCEEDED]이고, 작업은 대기열로 돌아가며 작업자는 잠시 멈춘다.
     * 동시에 채점하는 작업들이 각자 확인을 통과해 함께 한도를 넘지 않게 하기 위해서다. 작업이 끝나면 [release]로 푼다.
     * 확인과 예약 사이에 다른 예약·기록이 끼지 않도록, 부르는 쪽이 [record]와 같은 잠금 안에서 부른다.
     *
     * 맨 앞 작업의 어림 비용이 남은 한도를 넘어도 한도에 맞는 뒤의 작은 작업을 먼저 잡지 않는다. 그렇게 하면 긴 대화를 낸
     * 앞 작업이 짧은 작업들에 계속 밀려 24시간 만료로 끝날 수 있고, 처리 순서가 제출 순서와 어긋나기 때문이다.
     * 하루 한도는 한국 시각 자정에 풀리므로, 멈춘 뒤 다시 확인해 제출 순서대로 처리한다.
     */
    fun reserve(estimate: ModelUsage, now: Instant) {
        val period = AiBudgetPeriod.at(now)
        val today = usageBetween(period.dayKey(), period.dayKey())
        val thisMonth = usageBetween(period.monthStartKey(), period.dayKey())
        if (budget.wouldExceed(today, thisMonth, reservations.total() + estimate)) throw SantaCutException(ErrorCode.AI_BUDGET_EXCEEDED)
        reservations.add(estimate)
    }

    /** 끝난 작업의 예약을 푼다. 실제 사용량은 [record]로 이미 더했다. */
    fun release(estimate: ModelUsage) = reservations.remove(estimate)

    /** 호출 한 번의 사용량을 오늘 합계에 더한다. 사용량을 받지 못한 호출([ModelUsage.NONE])은 더하지 않는다. */
    fun record(usage: ModelUsage, now: Instant) {
        if (!usage.isMeasured()) return
        val key = AiBudgetPeriod.at(now).dayKey()
        val day = aiUsageDayRepository.findById(key).orElse(null) ?: AiUsageDay(key)
        day.add(usage)
        aiUsageDayRepository.save(day)
    }

    /** 실패 종류에 따라 바로 멈추거나 연속 실패로 센다. 규칙은 [ProviderBackoff]가 정한다. */
    fun recordFailure(failure: ScoringFailure, now: Instant) = backoff.record(failure, now)

    fun recordSuccess() = backoff.recordSuccess()

    private fun usageBetween(fromKey: String, toKey: String): AiUsageDays =
        AiUsageDays(aiUsageDayRepository.findAllByUsageDateBetween(fromKey, toKey))
}
