package com.santacut.infrastructure.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 점수 산정에 쓰는 Ollama 모델 이름. 비어 있으면 AI 분석을 끈다.
 */
@ConfigurationProperties(prefix = "spring.ai.ollama.chat.options")
data class ScoreModelProperties(
    val model: String = DEFAULT_MODEL,
) {
    companion object {
        private const val DEFAULT_MODEL = "qwen2.5:3b"
    }
}
