package com.santacut.infrastructure.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 점수 산정에 쓰는 Gemini 모델 설정.
 *
 * @property model 모델 ID. 비어 있으면 AI 분석을 끈다.
 * @property maxOutputTokens 호출 한 번의 출력 토큰 상한. 비용 가드가 호출 전 어림 비용을 셀 때 쓴다.
 */
@ConfigurationProperties(prefix = "spring.ai.google.genai.chat")
data class ScoreModelProperties(
    val model: String = DEFAULT_MODEL,
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
) {
    companion object {
        private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
        private const val DEFAULT_MAX_OUTPUT_TOKENS = 250
    }
}
