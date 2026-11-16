package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * AI 비용 한도와 토큰 단가. 단가는 설정값이며 실제 청구 단가와 다를 수 있다. 하루 한도는 없을 수 있다.
 */
class AiBudget(
    private val dailyUsd: BigDecimal?,
    private val monthlyUsd: BigDecimal,
    private val inputUsdPerMillionTokens: BigDecimal,
    private val outputUsdPerMillionTokens: BigDecimal,
) {
    /** 오늘 쓴 비용이나 이번 달 쓴 비용이 한도에 닿았는지. 하루 한도가 없으면 한 달 한도만 본다. */
    fun isExhausted(today: AiUsageDays, thisMonth: AiUsageDays): Boolean =
        reaches(today.costUsd(this), dailyLimit()) || reaches(thisMonth.costUsd(this), monthlyUsd)

    /** 오늘이나 이번 달 쓴 비용에 [estimate] 호출 한 번의 비용을 더하면 한도를 넘는지. */
    fun wouldExceed(today: AiUsageDays, thisMonth: AiUsageDays, estimate: ModelUsage): Boolean {
        val callCost = costUsd(estimate.inputTokens, estimate.billedOutputTokens())
        return exceeds(today.costUsd(this) + callCost, dailyLimit()) || exceeds(thisMonth.costUsd(this) + callCost, monthlyUsd)
    }

    /** 하루 한도. 설정하지 않았거나 0 이하면 null(한도 없음)이다. */
    private fun dailyLimit(): BigDecimal? = dailyUsd?.takeIf { it > BigDecimal.ZERO }

    private fun reaches(cost: BigDecimal, limit: BigDecimal?): Boolean = limit != null && cost >= limit

    private fun exceeds(cost: BigDecimal, limit: BigDecimal?): Boolean = limit != null && cost > limit

    /** 입력·출력 토큰 수로 어림한 비용(달러). */
    fun costUsd(inputTokens: Long, outputTokens: Long): BigDecimal =
        (BigDecimal(inputTokens) * inputUsdPerMillionTokens + BigDecimal(outputTokens) * outputUsdPerMillionTokens)
            .divide(TOKENS_PER_PRICE_UNIT)

    companion object {
        private val TOKENS_PER_PRICE_UNIT = BigDecimal(1_000_000)
    }
}
