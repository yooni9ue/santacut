package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.AnalysisEvent
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Bedrock 호출 경계(ChatModel)를 바꿔 끼우고 ScoreClient가 응답을 읽는 방식을 확인한다.
 */
class ScoreClientTest {
    private val chatModel = RecordingChatModel(FULL_ANSWER)
    private val scoreClient = ScoreClient(JsonMapper.builder().build(), ChatClient.builder(chatModel), ScoreModelProperties())

    @Test
    fun `JSON 응답을 읽어 점수로 바꾼다`() {
        assertEquals(EXPECTED_HUNDREDTHS, scoreClient.requestAnswer(submission(), SYSTEM_PROMPT).toScore(EVENT_COUNT).hundredths)
    }

    @Test
    fun `코드 블록으로 감싼 JSON도 읽는다`() {
        chatModel.reply = "```json\n$FULL_ANSWER\n```"

        assertEquals(EXPECTED_HUNDREDTHS, scoreClient.requestAnswer(submission(), SYSTEM_PROMPT).toScore(EVENT_COUNT).hundredths)
    }

    @Test
    fun `JSON 객체가 없는 응답은 AI_RESPONSE_INVALID다`() {
        chatModel.reply = "점수를 매길 수 없습니다."

        val exception = assertThrows<SantaCutException> { scoreClient.requestAnswer(submission(), SYSTEM_PROMPT).toScore(EVENT_COUNT) }

        assertEquals(ErrorCode.AI_RESPONSE_INVALID, exception.errorCode)
    }

    @Test
    fun `근거 부족 응답은 INSUFFICIENT_EVIDENCE다`() {
        chatModel.reply = """{"sufficient":false,"reason":"INSUFFICIENT_EVIDENCE"}"""

        val exception = assertThrows<SantaCutException> { scoreClient.requestAnswer(submission(), SYSTEM_PROMPT).toScore(EVENT_COUNT) }

        assertEquals(ErrorCode.INSUFFICIENT_EVIDENCE, exception.errorCode)
    }

    @Test
    fun `넘겨받은 프롬프트를 시스템 메시지로, 번호 붙인 이벤트를 사용자 메시지로 보낸다`() {
        scoreClient.requestAnswer(submission(), SYSTEM_PROMPT)

        val prompt = checkNotNull(chatModel.lastPrompt)
        assertEquals(SYSTEM_PROMPT, prompt.systemMessage.text)
        assertTrue(prompt.userMessage.text.orEmpty().contains("\"index\":1"))
    }

    @Test
    fun `모델 ID가 비어 있으면 꺼진 것으로 본다`() {
        val disabled = ScoreClient(JsonMapper.builder().build(), ChatClient.builder(chatModel), ScoreModelProperties(""))

        assertFalse(disabled.isEnabled())
        assertTrue(scoreClient.isEnabled())
    }

    private fun submission(): AnalysisSubmission = AnalysisSubmission(
        DataSource.KAKAO,
        AnalysisEvents(List(EVENT_COUNT) { index -> AnalysisEvent(OCCURRED_AT, "고마워 $index") }),
    )

    companion object {
        private const val EVENT_COUNT = 4
        private const val EXPECTED_HUNDREDTHS = 8000
        private const val SYSTEM_PROMPT = "테스트 채점 프롬프트"
        private val OCCURRED_AT: Instant = Instant.parse("2026-11-01T00:00:00Z")
        private const val FULL_ANSWER = """{"sufficient":true,""" +
            """"scores":{"care_empathy":80,"positivity_support":80,"respectful_communication":80,"cooperation_sharing":80},""" +
            """"evidence":{"care_empathy":[1],"positivity_support":[2],"respectful_communication":[3],"cooperation_sharing":[4]}}"""
    }
}
