package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 여러 날의 AI 사용량. 하루나 한 달 비용을 더할 때 쓴다.
 */
class AiUsageDays(private val values: List<AiUsageDay>) {
    fun costUsd(budget: AiBudget): BigDecimal = values.fold(BigDecimal.ZERO) { total, day -> total + day.costUsd(budget) }
}
