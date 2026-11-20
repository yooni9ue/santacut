package com.santacut.infrastructure.roast.client

import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.roast.model.RoastAnswer
import com.santacut.infrastructure.analysis.client.ExceptionCauses
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.global.config.RoastModelProperties
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage
import org.springframework.stereotype.Component

/**
 * 「팩폭 산타」 작가 모델(Gemini)에 시스템 프롬프트와 서버가 조립한 입력 JSON을 보내고 응답 본문을 그대로 돌려준다.
 * 모델·온도·출력 한도·생각 수준은 채점과 따로 [RoastModelProperties]에서 읽고, 응답 형태는 구조화 출력([RoastResponseSchema])으로 강제한다.
 * 검증은 도메인 모델이 한다. 호출이 실패해도(제공자 오류, 시간 초과) 예외를 던지지 않고 본문 없는 답을 돌려줘 다시 쓰기나 대체 문장으로 넘어가게 한다.
 * 프롬프트, 입력, 응답 본문, 제공자 메시지는 로그에 남기지 않는다.
 */
@Component
class RoastClient(
    chatClientBuilder: ChatClient.Builder,
    private val properties: RoastModelProperties,
) {
    private val chatClient = chatClientBuilder.build()
    private val logger = LoggerFactory.getLogger(RoastClient::class.java)

    fun isEnabled(): Boolean = properties.model.isNotBlank()

    fun model(): String = properties.model

    /** 호출 한 번의 어림 사용량. 입력은 글자 수로, 출력은 어림 출력 토큰 수([RoastModelProperties.estimatedOutputTokens])로 잡는다. */
    fun estimateUsage(systemPrompt: String, userText: String): ModelUsage = ModelUsage(
        ScoreClient.estimatedTokens(systemPrompt.length + userText.length), properties.estimatedOutputTokens.toLong(), ESTIMATED_FINISH_REASON,
    )

    /** [systemPrompt]를 시스템 메시지로, [userText]를 사용자 메시지로 보낸다. 실패하면 본문 없는 실패 답이다(사용량 0). */
    fun request(systemPrompt: String, userText: String): RoastAnswer =
        try {
            answerOf(chatClient.prompt().options(options()).system(systemPrompt).user(userText).call().chatResponse())
        } catch (exception: RuntimeException) {
            val causes = ExceptionCauses.of(exception)
            logger.warn("팩폭 작가 호출 실패(제공자 문제 {}): {}", causes.isProviderUnavailable(), exception.javaClass.simpleName)
            RoastAnswer(null, ModelUsage(0, 0, FAILED_FINISH_REASON), failed = true)
        }

    private fun options(): GoogleGenAiChatOptions.Builder {
        val builder = GoogleGenAiChatOptions.builder()
            .model(properties.model)
            .temperature(properties.temperature)
            .maxOutputTokens(properties.maxOutputTokens)
            .responseMimeType(JSON_MIME_TYPE)
            .responseSchema(RoastResponseSchema.JSON)
        if (properties.thinkingLevel.isNotBlank()) builder.thinkingLevel(GoogleGenAiThinkingLevel.valueOf(properties.thinkingLevel))
        return builder
    }

    private fun answerOf(response: ChatResponse?): RoastAnswer = RoastAnswer(response?.result?.output?.text, usageOf(response))

    private fun usageOf(response: ChatResponse?): ModelUsage {
        val usage = response?.metadata?.usage
        val genAiUsage = usage as? GoogleGenAiUsage
        return ModelUsage(
            (usage?.promptTokens ?: 0).toLong(),
            (usage?.completionTokens ?: 0).toLong(),
            response?.result?.metadata?.finishReason.orEmpty(),
            (genAiUsage?.thoughtsTokenCount ?: 0).toLong(),
            (genAiUsage?.cachedContentTokenCount ?: 0).toLong(),
        )
    }

    companion object {
        private const val JSON_MIME_TYPE = "application/json"
        private const val ESTIMATED_FINISH_REASON = "ESTIMATE"
        private const val FAILED_FINISH_REASON = "FAILED"
    }
}
