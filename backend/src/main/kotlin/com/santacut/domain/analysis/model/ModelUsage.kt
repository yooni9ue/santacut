package com.santacut.domain.analysis.model

/**
 * AI 호출 한 번의 토큰 사용량과 응답 종료 사유. 응답 메타데이터에서 읽은 숫자만 담고 본문은 담지 않는다.
 *
 * @property finishReason 모델이 알려 준 종료 사유(예: `STOP`, `MAX_TOKENS`). 모르면 빈 문자열이다.
 */
data class ModelUsage(
    val inputTokens: Long,
    val outputTokens: Long,
    val finishReason: String,
) {
    /** 사용량을 실제로 받은 호출인지. [NONE]은 비용 합계에 더하지 않는다. */
    fun isMeasured(): Boolean = this != NONE

    companion object {
        /** 호출하지 못했거나 사용량을 받지 못했을 때. */
        val NONE = ModelUsage(0, 0, "")
    }
}
