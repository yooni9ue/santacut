package com.santacut.infrastructure.analysis.client

import com.google.genai.errors.ClientException
import com.santacut.domain.analysis.model.AnalysisEvent
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Gemini 호출 경계(ChatModel)를 바꿔 끼우고 ScoreClient가 응답과 사용량을 읽는 방식을 확인한다.
 */
class ScoreClientTest {
    private val chatModel = RecordingChatModel(FULL_ANSWER)
    private val scoreClient = ScoreClient(JsonMapper.builder().build(), ChatClient.builder(chatModel), ScoreModelProperties())

    @Test
    fun `JSON 응답을 읽어 점수로 바꾼다`() {
        assertEquals(EXPECTED_HUNDREDTHS, scoreClient.requestAnswer(digest(), SYSTEM_PROMPT).toScore(EVENT_COUNT).hundredths)
    }

    @Test
    fun `코드 블록으로 감싼 JSON도 읽는다`() {
        chatModel.reply = "```json\n$FULL_ANSWER\n```"

        assertEquals(EXPECTED_HUNDREDTHS, scoreClient.requestAnswer(digest(), SYSTEM_PROMPT).toScore(EVENT_COUNT).hundredths)
    }

    @Test
    fun `응답 메타데이터의 토큰 사용량과 종료 사유를 답에 담는다`() {
        val usage = scoreClient.requestAnswer(digest(), SYSTEM_PROMPT).usage

        assertEquals(
            ModelUsage(
                RecordingChatModel.INPUT_TOKENS.toLong(),
                RecordingChatModel.OUTPUT_TOKENS.toLong(),
                RecordingChatModel.FINISH_REASON,
            ),
            usage,
        )
    }

    @Test
    fun `Gemini 사용량의 생각 토큰을 답 본문 토큰과 따로 담는다`() {
        chatModel.usage = GoogleGenAiUsage(
            RecordingChatModel.INPUT_TOKENS, RecordingChatModel.OUTPUT_TOKENS, TOTAL_TOKENS, THOUGHT_TOKENS,
            null, null, null, null, null, null, null, null,
        )

        val usage = scoreClient.requestAnswer(digest(), SYSTEM_PROMPT).usage

        assertEquals(THOUGHT_TOKENS.toLong(), usage.thoughtTokens)
        assertEquals((RecordingChatModel.OUTPUT_TOKENS + THOUGHT_TOKENS).toLong(), usage.billedOutputTokens())
    }

    @Test
    fun `JSON 객체가 없거나 잘린 JSON이면 AI_RESPONSE_INVALID이고 사용량은 남긴다`() {
        listOf("점수를 매길 수 없습니다.", """{"sufficient":true,"evidence":{"care_empathy":[1}""").forEach { reply ->
            chatModel.reply = reply

            val answer = scoreClient.requestAnswer(digest(), SYSTEM_PROMPT)

            assertEquals(ErrorCode.AI_RESPONSE_INVALID, assertThrows<SantaCutException> { answer.toScore(EVENT_COUNT) }.errorCode)
            assertEquals(RecordingChatModel.INPUT_TOKENS.toLong(), answer.usage.inputTokens)
        }
    }

    @Test
    fun `근거 부족 응답은 INSUFFICIENT_EVIDENCE다`() {
        chatModel.reply = """{"sufficient":false,"reason":"INSUFFICIENT_EVIDENCE"}"""

        val exception = assertThrows<SantaCutException> { scoreClient.requestAnswer(digest(), SYSTEM_PROMPT).toScore(EVENT_COUNT) }

        assertEquals(ErrorCode.INSUFFICIENT_EVIDENCE, exception.errorCode)
    }

    @Test
    fun `제공자가 요청 한도로 막으면 던지지 않고 AI_PROVIDER_UNAVAILABLE 답을 돌려준다`() {
        chatModel.failure = IllegalStateException("감싼 예외", ClientException(429, "RESOURCE_EXHAUSTED", "한도"))

        val answer = scoreClient.requestAnswer(digest(), SYSTEM_PROMPT)

        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, assertThrows<SantaCutException> { answer.toScore(EVENT_COUNT) }.errorCode)
        assertEquals(ModelUsage.NONE, answer.usage)
    }

    @Test
    fun `그 밖의 호출 오류는 그대로 던진다`() {
        chatModel.failure = IllegalStateException("AI 장애")

        assertThrows<IllegalStateException> { scoreClient.requestAnswer(digest(), SYSTEM_PROMPT) }
    }

    @Test
    fun `넘겨받은 프롬프트를 시스템 메시지로, 신호와 번호 붙인 표본 줄을 사용자 메시지로 보낸다`() {
        scoreClient.requestAnswer(digest(), SYSTEM_PROMPT)

        val prompt = checkNotNull(chatModel.lastPrompt)
        val userText = prompt.userMessage.text.orEmpty()
        assertEquals(SYSTEM_PROMPT, prompt.systemMessage.text)
        assertTrue(userText.startsWith("[signals]\ncare_empathy hit=0 neg=0 weeks=0/1 shown=0\npositivity_support hit=4 neg=0 weeks=1/1 shown=4\n"))
        assertTrue(userText.endsWith("[sample]\n# 10-26주\n1|고마워 0\n2|고마워 1\n3|고마워 2\n4|고마워 3"))
    }

    @Test
    fun `캐시 키는 프롬프트 판이 다르면 달라진다`() {
        assertEquals(scoreClient.cacheKey(digest(), 2), scoreClient.cacheKey(digest(), 2))
        assertNotEquals(scoreClient.cacheKey(digest(), 2), scoreClient.cacheKey(digest(), 3))
    }

    @Test
    fun `모델 ID가 비어 있으면 꺼진 것으로 본다`() {
        val disabled = ScoreClient(JsonMapper.builder().build(), ChatClient.builder(chatModel), ScoreModelProperties(""))

        assertFalse(disabled.isEnabled())
        assertTrue(scoreClient.isEnabled())
    }

    private fun digest(): ScoringDigest = AnalysisSubmission(
        DataSource.KAKAO,
        AnalysisEvents(List(EVENT_COUNT) { index -> AnalysisEvent(OCCURRED_AT, "고마워 $index") }),
    ).toScoringDigest()

    companion object {
        private const val EVENT_COUNT = 4
        private const val EXPECTED_HUNDREDTHS = 8000
        private const val SYSTEM_PROMPT = "테스트 채점 프롬프트"
        private const val THOUGHT_TOKENS = 37
        private const val TOTAL_TOKENS = RecordingChatModel.INPUT_TOKENS + RecordingChatModel.OUTPUT_TOKENS + THOUGHT_TOKENS
        private val OCCURRED_AT: Instant = Instant.parse("2026-11-01T00:00:00Z")
        private const val FULL_ANSWER = """{"sufficient":true,""" +
            """"evidence":{"care_empathy":[1],"positivity_support":[2],"respectful_communication":[3],"cooperation_sharing":[4]},""" +
            """"scores":{"care_empathy":80,"positivity_support":80,"respectful_communication":80,"cooperation_sharing":80}}"""
    }
}
