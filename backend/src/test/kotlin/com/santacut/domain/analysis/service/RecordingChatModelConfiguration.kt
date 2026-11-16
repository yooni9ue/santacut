package com.santacut.domain.analysis.service

import com.santacut.infrastructure.analysis.client.RecordingChatModel
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Bedrock 대신 정해 둔 답을 돌려주고 마지막 프롬프트를 기억하는 채팅 모델을 AI 호출 경계(ChatModel)에 끼운다.
 */
@TestConfiguration
class RecordingChatModelConfiguration {
    @Bean
    @Primary
    fun recordingChatModel(): RecordingChatModel = RecordingChatModel(FULL_ANSWER)

    companion object {
        /** 모든 항목 80점, 근거는 표본 1~4번 줄. */
        const val FULL_ANSWER = """{"sufficient":true,""" +
            """"scores":{"care_empathy":80,"positivity_support":80,"respectful_communication":80,"cooperation_sharing":80},""" +
            """"evidence":{"care_empathy":[1],"positivity_support":[2],"respectful_communication":[3],"cooperation_sharing":[4]}}"""
    }
}
