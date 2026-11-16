package com.santacut.infrastructure.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 점수 산정에 쓰는 Gemini 모델 ID. 비어 있으면 AI 분석을 끈다.
 */
@ConfigurationProperties(prefix = "spring.ai.google.genai.chat")
data class ScoreModelProperties(
    val model: String = DEFAULT_MODEL,
) {
    companion object {
        private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
    }
}
