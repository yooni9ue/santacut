package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * AI 비용 한도와 토큰 단가. 단가는 설정값이며 실제 청구 단가와 다를 수 있다.
 */
class AiBudget(
    private val dailyUsd: BigDecimal,
    private val monthlyUsd: BigDecimal,
    private val inputUsdPerMillionTokens: BigDecimal,
    private val outputUsdPerMillionTokens: BigDecimal,
) {
    /** 오늘 쓴 비용이나 이번 달 쓴 비용이 한도에 닿았는지. */
    fun isExhausted(today: AiUsageDays, thisMonth: AiUsageDays): Boolean =
        today.costUsd(this) >= dailyUsd || thisMonth.costUsd(this) >= monthlyUsd

    /** 입력·출력 토큰 수로 어림한 비용(달러). */
    fun costUsd(inputTokens: Long, outputTokens: Long): BigDecimal =
        (BigDecimal(inputTokens) * inputUsdPerMillionTokens + BigDecimal(outputTokens) * outputUsdPerMillionTokens)
            .divide(TOKENS_PER_PRICE_UNIT)

    companion object {
        private val TOKENS_PER_PRICE_UNIT = BigDecimal(1_000_000)
    }
}
