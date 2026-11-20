package com.santacut.infrastructure.roast.client

import com.google.genai.types.GenerateContentResponseUsageMetadata
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.infrastructure.analysis.client.RecordingChatModel
import com.santacut.infrastructure.global.config.RoastModelProperties
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage
import kotlin.test.assertEquals

/**
 * Gemini 호출 경계(ChatModel)를 바꿔 끼우고 RoastClient가 보내는 옵션과 응답 사용량을 읽는 방식을 확인한다.
 * 작가 로그의 생각 토큰이 늘 0이던 원인을 가리기 위해, 생각 수준이 요청에 실리는지와 thoughtsTokenCount가 그대로 옮겨지는지를 따로 본다.
 */
class RoastClientTest {
    private val chatModel = RecordingChatModel(REPLY)
    private val roastClient = RoastClient(ChatClient.builder(chatModel), RoastModelProperties())

    @Test
    fun `작가 모델, 생각 수준, 응답 스키마를 기본 옵션과 합친 요청 옵션으로 보낸다`() {
        roastClient.request(SYSTEM_PROMPT, USER_TEXT)

        val options = chatModel.lastPrompt?.options as GoogleGenAiChatOptions
        assertEquals(DEFAULT_MODEL, options.model)
        assertEquals(GoogleGenAiThinkingLevel.LOW, options.thinkingLevel)
        assertEquals(RoastResponseSchema.JSON, options.responseSchema)
    }

    @Test
    fun `응답 사용량의 생각 토큰과 캐시 읽기 토큰을 그대로 옮긴다`() {
        chatModel.usage = GoogleGenAiUsage.from(
            GenerateContentResponseUsageMetadata.builder()
                .promptTokenCount(INPUT_TOKENS)
                .candidatesTokenCount(OUTPUT_TOKENS)
                .thoughtsTokenCount(THOUGHT_TOKENS)
                .cachedContentTokenCount(CACHED_TOKENS)
                .totalTokenCount(INPUT_TOKENS + OUTPUT_TOKENS + THOUGHT_TOKENS)
                .build(),
        )

        val usage = roastClient.request(SYSTEM_PROMPT, USER_TEXT).usage

        assertEquals(
            ModelUsage(INPUT_TOKENS.toLong(), OUTPUT_TOKENS.toLong(), RecordingChatModel.FINISH_REASON, THOUGHT_TOKENS.toLong(), CACHED_TOKENS.toLong()),
            usage,
        )
    }

    @Test
    fun `제공자가 생각 토큰 수를 싣지 않으면 0으로 남는다`() {
        chatModel.usage = GoogleGenAiUsage.from(
            GenerateContentResponseUsageMetadata.builder()
                .promptTokenCount(INPUT_TOKENS)
                .candidatesTokenCount(OUTPUT_TOKENS)
                .totalTokenCount(INPUT_TOKENS + OUTPUT_TOKENS)
                .build(),
        )

        assertEquals(0, roastClient.request(SYSTEM_PROMPT, USER_TEXT).usage.thoughtTokens)
    }

    companion object {
        private const val REPLY = "{}"
        private const val SYSTEM_PROMPT = "작가 프롬프트"
        private const val USER_TEXT = "{}"
        private const val DEFAULT_MODEL = "gemini-3.5-flash"
        private const val INPUT_TOKENS = 7_314
        private const val OUTPUT_TOKENS = 522
        private const val THOUGHT_TOKENS = 321
        private const val CACHED_TOKENS = 2_048
    }
}
