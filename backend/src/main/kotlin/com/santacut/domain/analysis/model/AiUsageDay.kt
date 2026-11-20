package com.santacut.domain.analysis.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 하루(한국 날짜) 동안의 AI 토큰 사용량 합계. 앱 안의 비용 가드가 읽는다. 숫자만 담고 본문은 담지 않는다.
 * 채점(`input_tokens` 등)과 작가(`roast_*`)는 모델 단가가 달라 따로 더한다. 작가 컬럼이 생기기 전 작가 사용량은 채점 컬럼에 섞여 있다.
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

    @Column(name = "input_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var inputTokens: Long = 0
        protected set

    /** 입력 토큰 중 암묵적 캐시에서 읽은 토큰 수. 기존 DB에는 Hibernate가 기본값 0으로 컬럼을 더한다. */
    @Column(name = "cached_input_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var cachedInputTokens: Long = 0
        protected set

    @Column(name = "output_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var outputTokens: Long = 0
        protected set

    @Column(name = "calls", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var calls: Int = 0
        protected set

    /** 작가 호출의 입력 토큰 수. 위의 채점 토큰과 따로 작가 모델 단가로 센다. 기존 DB에는 Hibernate가 기본값 0으로 컬럼을 더한다. */
    @Column(name = "roast_input_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var roastInputTokens: Long = 0
        protected set

    @Column(name = "roast_cached_input_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var roastCachedInputTokens: Long = 0
        protected set

    /** 작가 호출의 답 본문 토큰 수. 생각 토큰은 [roastThoughtTokens]에 따로 둔다. */
    @Column(name = "roast_output_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var roastOutputTokens: Long = 0
        protected set

    @Column(name = "roast_thought_tokens", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var roastThoughtTokens: Long = 0
        protected set

    @Column(name = "roast_calls", nullable = false, columnDefinition = ZERO_COLUMN_DEFINITION)
    var roastCalls: Int = 0
        protected set

    /** [kind] AI 호출 한 번의 사용량을 더한다. */
    fun add(usage: ModelUsage, kind: AiCallKind) = when (kind) {
        AiCallKind.SCORE -> addScore(usage)
        AiCallKind.ROAST -> addRoast(usage)
    }

    /** 채점 호출 사용량과 작가 호출 사용량을 각 모델 단가로 어림한 비용의 합(달러). */
    fun costUsd(budget: AiBudget): BigDecimal =
        budget.priceOf(AiCallKind.SCORE).costUsd(inputTokens, cachedInputTokens, outputTokens, NO_SEPARATE_THOUGHTS) +
            budget.priceOf(AiCallKind.ROAST).costUsd(roastInputTokens, roastCachedInputTokens, roastOutputTokens, roastThoughtTokens)

    /** 채점 호출. 출력 토큰에는 같은 단가로 청구되는 생각 토큰을 함께 더한다(이 컬럼이 생긴 때부터의 방식). */
    private fun addScore(usage: ModelUsage) {
        inputTokens += usage.inputTokens
        cachedInputTokens += usage.cachedInputTokens
        outputTokens += usage.billedOutputTokens()
        calls++
    }

    private fun addRoast(usage: ModelUsage) {
        roastInputTokens += usage.inputTokens
        roastCachedInputTokens += usage.cachedInputTokens
        roastOutputTokens += usage.outputTokens
        roastThoughtTokens += usage.thoughtTokens
        roastCalls++
    }

    companion object {
        private const val ZERO_COLUMN_DEFINITION = "INTEGER NOT NULL DEFAULT 0"

        /** 채점 생각 토큰은 [outputTokens]에 이미 들어 있다. */
        private const val NO_SEPARATE_THOUGHTS = 0L

        fun keyOf(date: LocalDate): String = date.toString()
    }
}
