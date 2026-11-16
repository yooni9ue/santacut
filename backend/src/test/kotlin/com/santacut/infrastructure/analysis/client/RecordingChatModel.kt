package com.santacut.infrastructure.analysis.client

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

/**
 * Bedrock 대신 정해 둔 답을 돌려주고, 마지막으로 받은 프롬프트를 기억하는 테스트용 채팅 모델.
 *
 * @property reply 모델이 돌려줄 답 본문
 */
class RecordingChatModel(var reply: String) : ChatModel {
    var lastPrompt: Prompt? = null
        private set

    override fun call(prompt: Prompt): ChatResponse {
        lastPrompt = prompt
        return ChatResponse(listOf(Generation(AssistantMessage(reply))))
    }
}
