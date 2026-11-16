package com.santacut.infrastructure.global.config

import com.google.genai.Client
import com.google.genai.types.HttpOptions
import com.google.genai.types.HttpRetryOptions
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeminiClientConfiguration {
    /**
     * Gemini Developer API 클라이언트. 키는 `spring.ai.google.genai.api-key`(환경 변수 GEMINI_API_KEY)에서 읽는다.
     * 키가 비어 있으면 SDK가 환경 변수에서 다시 찾고, 그래도 없으면 앱이 뜨지 않는다.
     * 호출 하나의 시간 상한은 [ScoreModelProperties.callTimeout]이고 SDK 재시도는 끈다. 작업 리스가 이 상한과 구간 수 상한으로 정해지므로
     * 리스가 끝난 뒤 같은 작업을 또 부르지 않는다.
     * 다시 시도할지는 앱의 ProviderBackoff가 정한다.
     */
    @Bean
    fun googleGenAiClient(connectionProperties: GoogleGenAiConnectionProperties, scoreModelProperties: ScoreModelProperties): Client =
        Client.builder()
            .apply { connectionProperties.apiKey?.takeIf { it.isNotBlank() }?.let { apiKey(it) } }
            .httpOptions(
                HttpOptions.builder()
                    .timeout(scoreModelProperties.callTimeout.toMillis().toInt())
                    .retryOptions(HttpRetryOptions.builder().attempts(NO_RETRY_ATTEMPTS))
                    .build(),
            )
            .build()

    companion object {
        private const val NO_RETRY_ATTEMPTS = 1
    }
}
