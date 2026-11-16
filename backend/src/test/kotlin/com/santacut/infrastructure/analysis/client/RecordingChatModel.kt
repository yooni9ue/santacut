package com.santacut.infrastructure.analysis.client

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

/**
 * Gemini 대신 정해 둔 답과 토큰 사용량을 돌려주고, 마지막으로 받은 프롬프트를 기억하는 테스트용 채팅 모델.
 *
 * @property reply 모델이 돌려줄 답 본문
 * @property failure 있으면 답 대신 이 예외를 던진다
 * @property usage 응답 메타데이터에 실을 토큰 사용량
 */
class RecordingChatModel(var reply: String) : ChatModel {
    var failure: RuntimeException? = null

    var usage: Usage = DefaultUsage(INPUT_TOKENS, OUTPUT_TOKENS)

    var lastPrompt: Prompt? = null
        private set

    /** 받은 모든 프롬프트. 긴 대화는 구간마다 한 번씩 부른다. */
    val prompts = mutableListOf<Prompt>()

    override fun call(prompt: Prompt): ChatResponse {
        lastPrompt = prompt
        prompts += prompt
        failure?.let { throw it }
        val generation = Generation(AssistantMessage(reply), ChatGenerationMetadata.builder().finishReason(FINISH_REASON).build())
        return ChatResponse(listOf(generation), ChatResponseMetadata.builder().usage(usage).build())
    }

    companion object {
        const val INPUT_TOKENS = 4200
        const val OUTPUT_TOKENS = 90
        const val FINISH_REASON = "end_turn"
    }
}
