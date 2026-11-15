package com.santacut.presentation.analysis.controller

import com.jayway.jsonpath.JsonPath
import com.santacut.IntegrationTest
import com.santacut.TestChatFiles
import com.santacut.TestChatFiles.SELF_SPEAKER
import com.santacut.TestCsrf.withCsrf
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

/**
 * 원본 파일 업로드 경로의 HTTP 계약(상태 코드, 오류 코드, 재전송 처리)을 확인한다.
 */
@IntegrationTest
class AnalysisUploadApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val clock: Clock,
) {
    private val chat = TestChatFiles.kakaoIos(clock.instant(), EVENT_COUNT)
    private val occurredAt = clock.instant().minus(Duration.ofDays(1))

    @Test
    fun `원본 파일을 받으면 202와 VALIDATING을 주고 같은 요청을 다시 보내면 같은 작업을 준다`() {
        val session = registerSession()
        val key = newKey()

        val first = mockMvc.perform(uploadRequest(session, key, chat))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.status").value("VALIDATING"))
            .andReturn().response.contentAsString
        val replay = mockMvc.perform(uploadRequest(session, key, chat))
            .andExpect(status().isAccepted)
            .andReturn().response.contentAsString

        assertEquals(JsonPath.read<String>(first, "$.id"), JsonPath.read<String>(replay, "$.id"))
    }

    @Test
    fun `인스타그램 JSON 원본도 202와 VALIDATING으로 받는다`() {
        val instagram = TestChatFiles.instagram(clock.instant(), EVENT_COUNT)

        mockMvc.perform(uploadRequest(registerSession(), newKey(), instagram, source = INSTAGRAM_SOURCE))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.status").value("VALIDATING"))
    }

    @Test
    fun `같은 키로 다른 본인 이름이나 JSON 제출을 보내면 409와 IDEMPOTENCY_CONFLICT다`() {
        val session = registerSession()
        val key = newKey()
        mockMvc.perform(uploadRequest(session, key, chat)).andExpect(status().isAccepted)

        mockMvc.perform(uploadRequest(session, key, chat, selfSpeaker = "루돌프"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
        mockMvc.perform(jsonSubmitRequest(session, key))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
    }

    @Test
    fun `zip 파일이나 빈 파일은 422와 UNSUPPORTED_FILE_FORMAT이다`() {
        val session = registerSession()
        val zip = checkNotNull(javaClass.getResourceAsStream("/fixtures/upload/archive.zip")).use { it.readBytes() }
        mockMvc.perform(uploadRequest(session, newKey(), zip))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_FORMAT"))
        mockMvc.perform(uploadRequest(session, newKey(), ByteArray(0)))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_FORMAT"))
    }

    @Test
    fun `파일이나 본인 이름 파트가 없거나 출처가 틀리면 400과 INVALID_REQUEST다`() {
        val session = registerSession()
        val withoutFile = withCsrf(mockMvc, multipart(UPLOAD_PATH))
            .cookie(session).header(IDEMPOTENCY_KEY, newKey())
            .param(SOURCE_PART, SOURCE).param(SELF_SPEAKER_PART, SELF_SPEAKER)
        mockMvc.perform(withoutFile)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(uploadRequest(session, newKey(), chat, selfSpeaker = null))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(uploadRequest(session, newKey(), chat, source = "LINE"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `Idempotency-Key 형식이 틀리면 400과 INVALID_IDEMPOTENCY_KEY다`() {
        mockMvc.perform(uploadRequest(registerSession(), "short", chat))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"))
    }

    @Test
    fun `Idempotency-Key 헤더가 없으면 400과 INVALID_REQUEST다`() {
        val withoutKey = withCsrf(mockMvc, multipart(UPLOAD_PATH).file(chatPart(chat)))
            .cookie(registerSession())
            .param(SOURCE_PART, SOURCE).param(SELF_SPEAKER_PART, SELF_SPEAKER)
        mockMvc.perform(withoutKey)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `본인 이름이 비었거나 100자를 넘으면 400과 INVALID_REQUEST다`() {
        val session = registerSession()
        mockMvc.perform(uploadRequest(session, newKey(), chat, selfSpeaker = ""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(uploadRequest(session, newKey(), chat, selfSpeaker = "가".repeat(MAX_SELF_SPEAKER_LENGTH + 1)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `CSRF 토큰이 없으면 403, 세션이 없으면 401이다`() {
        val session = registerSession()
        mockMvc.perform(multipart(UPLOAD_PATH).file(chatPart(chat)).cookie(session).header(IDEMPOTENCY_KEY, newKey()))
            .andExpect(status().isForbidden)
        mockMvc.perform(withCsrf(mockMvc, multipart(UPLOAD_PATH).file(chatPart(chat))).header(IDEMPOTENCY_KEY, newKey()))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"))
    }

    private fun registerSession(): Cookie {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        return Cookie(SESSION_COOKIE, participant.sessionToken)
    }

    private fun uploadRequest(
        session: Cookie,
        key: String,
        content: ByteArray,
        selfSpeaker: String? = SELF_SPEAKER,
        source: String = SOURCE,
    ): MockMultipartHttpServletRequestBuilder {
        val request = withCsrf(mockMvc, multipart(UPLOAD_PATH).file(chatPart(content)))
            .cookie(session)
            .header(IDEMPOTENCY_KEY, key)
            .param(SOURCE_PART, source)
        return if (selfSpeaker == null) request else request.param(SELF_SPEAKER_PART, selfSpeaker)
    }

    private fun jsonSubmitRequest(session: Cookie, key: String): MockHttpServletRequestBuilder {
        val event = "{\"occurredAt\":\"$occurredAt\",\"text\":\"고마워\"}"
        val body = "{\"source\":\"$SOURCE\",\"events\":[" + List(EVENT_COUNT) { event }.joinToString(",") + "]}"
        return withCsrf(mockMvc, post("/v1/analyses"))
            .cookie(session)
            .header(IDEMPOTENCY_KEY, key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
    }

    private fun chatPart(content: ByteArray): MockMultipartFile =
        MockMultipartFile(FILE_PART, "KakaoTalkChats.txt", MediaType.TEXT_PLAIN_VALUE, content)

    private fun newKey(): String = "key-" + UUID.randomUUID()

    companion object {
        private const val UPLOAD_PATH = "/v1/analyses/files"
        private const val SESSION_COOKIE = "sc_session"
        private const val IDEMPOTENCY_KEY = "Idempotency-Key"
        private const val FILE_PART = "file"
        private const val SOURCE_PART = "source"
        private const val SELF_SPEAKER_PART = "self_speaker"
        private const val SOURCE = "KAKAO"
        private const val INSTAGRAM_SOURCE = "INSTAGRAM"
        private const val EVENT_COUNT = 20
        private const val MAX_SELF_SPEAKER_LENGTH = 100
    }
}
