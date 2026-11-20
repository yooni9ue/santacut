package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * AI 비용 한도와 호출 종류별 토큰 단가. 단가는 설정값이며 실제 청구 단가와 다를 수 있다. 하루 한도는 없을 수 있다.
 * 채점과 작가는 모델이 달라 단가도 다르므로 [AiCallKind]마다 따로 센다.
 */
class AiBudget(
    private val dailyUsd: BigDecimal?,
    private val monthlyUsd: BigDecimal,
    private val scorePrice: AiModelPrice,
    private val roastPrice: AiModelPrice,
) {
    /** 오늘 쓴 비용이나 이번 달 쓴 비용이 한도에 닿았는지. 하루 한도가 없으면 한 달 한도만 본다. */
    fun isExhausted(today: AiUsageDays, thisMonth: AiUsageDays): Boolean =
        reaches(today.costUsd(this), dailyLimit()) || reaches(thisMonth.costUsd(this), monthlyUsd)

    /** 오늘이나 이번 달 쓴 비용에 아직 쓰지 않은 어림 비용 [pendingCostUsd]를 더하면 한도를 넘는지. */
    fun wouldExceed(today: AiUsageDays, thisMonth: AiUsageDays, pendingCostUsd: BigDecimal): Boolean =
        exceeds(today.costUsd(this) + pendingCostUsd, dailyLimit()) || exceeds(thisMonth.costUsd(this) + pendingCostUsd, monthlyUsd)

    /** [kind] 호출의 사용량 [usage]를 그 모델 단가로 어림한 비용(달러). */
    fun costUsd(kind: AiCallKind, usage: ModelUsage): BigDecimal = priceOf(kind).costUsd(usage)

    /** [kind] 호출 모델의 단가. */
    fun priceOf(kind: AiCallKind): AiModelPrice = when (kind) {
        AiCallKind.SCORE -> scorePrice
        AiCallKind.ROAST -> roastPrice
    }

    /** 하루 한도. 설정하지 않았거나 0 이하면 null(한도 없음)이다. */
    private fun dailyLimit(): BigDecimal? = dailyUsd?.takeIf { it > BigDecimal.ZERO }

    private fun reaches(cost: BigDecimal, limit: BigDecimal?): Boolean = limit != null && cost >= limit

    private fun exceeds(cost: BigDecimal, limit: BigDecimal?): Boolean = limit != null && cost > limit
}
