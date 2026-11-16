package com.santacut.domain.analysis.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 하루(한국 날짜) 동안의 AI 토큰 사용량 합계. 앱 안의 비용 가드가 읽는다. 숫자만 담고 본문은 담지 않는다.
 *
 * @property usageDate `yyyy-MM-dd` 형식의 한국 날짜
 */
@Entity
@Table(name = "ai_usage_days")
class AiUsageDay(
    usageDate: String,
) {
    @Id
    @Column(name = "usage_date")
    var usageDate: String = usageDate
        protected set

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long = 0
        protected set

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long = 0
        protected set

    @Column(name = "calls", nullable = false)
    var calls: Int = 0
        protected set

    /** AI 호출 한 번의 사용량을 더한다. 출력 토큰에는 같은 단가로 청구되는 생각 토큰을 함께 더한다. */
    fun add(usage: ModelUsage) {
        inputTokens += usage.inputTokens
        outputTokens += usage.billedOutputTokens()
        calls++
    }

    fun costUsd(budget: AiBudget): BigDecimal = budget.costUsd(inputTokens, outputTokens)

    companion object {
        fun keyOf(date: LocalDate): String = date.toString()
    }
}
