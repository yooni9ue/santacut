package com.santacut.infrastructure.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 점수 산정에 쓰는 Bedrock 모델(추론 프로파일) ID. 비어 있으면 AI 분석을 끈다.
 */
@ConfigurationProperties(prefix = "spring.ai.bedrock.converse.chat")
data class ScoreModelProperties(
    val model: String = DEFAULT_MODEL,
) {
    companion object {
        private const val DEFAULT_MODEL = "global.anthropic.claude-haiku-4-5-20251001-v1:0"
    }
}
