package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 모델 하나의 토큰 단가(100만 토큰당 달러). 설정값이며 실제 청구 단가와 다를 수 있다.
 *
 * @property inputUsdPerMillionTokens 입력 토큰 단가
 * @property cachedInputUsdPerMillionTokens 암묵적 캐시에서 읽은 입력 토큰 단가
 * @property outputUsdPerMillionTokens 답 본문 토큰 단가
 * @property thinkingUsdPerMillionTokens 생각(thinking) 토큰 단가. Gemini는 출력 단가와 같다
 */
data class AiModelPrice(
    val inputUsdPerMillionTokens: BigDecimal,
    val cachedInputUsdPerMillionTokens: BigDecimal,
    val outputUsdPerMillionTokens: BigDecimal,
    val thinkingUsdPerMillionTokens: BigDecimal,
) {
    /** 토큰 수로 어림한 비용(달러). [cachedInputTokens]는 [inputTokens]에 든 캐시 읽기 토큰이며 캐시 단가로 센다. */
    fun costUsd(inputTokens: Long, cachedInputTokens: Long, outputTokens: Long, thoughtTokens: Long): BigDecimal =
        (
            BigDecimal(inputTokens - cachedInputTokens) * inputUsdPerMillionTokens +
                BigDecimal(cachedInputTokens) * cachedInputUsdPerMillionTokens +
                BigDecimal(outputTokens) * outputUsdPerMillionTokens +
                BigDecimal(thoughtTokens) * thinkingUsdPerMillionTokens
            ).divide(TOKENS_PER_PRICE_UNIT)

    /** 호출 한 번(또는 여러 호출 합)의 사용량 [usage]로 어림한 비용(달러). */
    fun costUsd(usage: ModelUsage): BigDecimal =
        costUsd(usage.inputTokens, usage.cachedInputTokens, usage.outputTokens, usage.thoughtTokens)

    companion object {
        private val TOKENS_PER_PRICE_UNIT = BigDecimal(1_000_000)
    }
}
