package com.santacut.infrastructure.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 점수 산정에 쓰는 Gemini 모델 설정.
 *
 * @property model 모델 ID. 비어 있으면 AI 분석을 끈다.
 * @property maxOutputTokens 호출 한 번의 출력 토큰 상한. 비용 가드가 호출 전 어림 비용을 셀 때 쓴다.
 * @property callTimeout 호출 하나의 시간 상한. 분석 작업 리스는 이 값과 구간 수 상한으로 정한다.
 */
@ConfigurationProperties(prefix = "spring.ai.google.genai.chat")
data class ScoreModelProperties(
    val model: String = DEFAULT_MODEL,
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
    val callTimeout: Duration = DEFAULT_CALL_TIMEOUT,
) {
    companion object {
        private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
        private const val DEFAULT_MAX_OUTPUT_TOKENS = 250
        private val DEFAULT_CALL_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}
