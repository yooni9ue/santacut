package com.santacut.presentation.participant.controller

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.TestChatFiles
import com.santacut.TestChatFiles.SELF_SPEAKER
import com.santacut.TestCsrf.withCsrf
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.UploadAnalysisCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.infrastructure.analysis.client.ScoreClient
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

/**
 * 같은 IP에서 참가자를 계속 만들어 참가자별 한도를 피하지 못하게 IP별 등록·제출 한도를 거는지 확인한다.
 * 한도를 낮춘 설정이라 `build/rate-limit-test.db`와 따로 뜬 컨텍스트를 쓴다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/rate-limit-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.rate-limit.registrations-per-client=2",
        "santacut.rate-limit.submissions-per-client=1",
    ],
)
@AutoConfigureMockMvc
@MockitoBean(types = [ScoreClient::class])
class ClientRateLimitApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val analysisFacade: AnalysisFacade,
    private val scoreClient: ScoreClient,
) {
    @Test
    fun `같은 IP의 참가 등록이 한도를 넘으면 429 CLIENT_RATE_LIMITED와 retry_at, Retry-After를 준다`() {
        register(REGISTERING_IP).andExpect(status().isCreated)
        register(REGISTERING_IP).andExpect(status().isCreated)

        register(REGISTERING_IP)
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("CLIENT_RATE_LIMITED"))
            .andExpect(jsonPath("$.retry_at").exists())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
        register(OTHER_IP).andExpect(status().isCreated)
    }

    @Test
    fun `참가자가 달라도 같은 IP의 분석 제출이 한도를 넘으면 CLIENT_RATE_LIMITED다`() {
        given(scoreClient.isEnabled()).willReturn(true)
        analysisFacade.upload(uploadCommand(registerParticipant()))

        assertErrorCode(ErrorCode.CLIENT_RATE_LIMITED) { analysisFacade.upload(uploadCommand(registerParticipant())) }
    }

    private fun register(ip: String): ResultActions = mockMvc.perform(
        withCsrf(mockMvc, post("/v1/participants"))
            .with { it.apply { remoteAddr = ip } }
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"nickname":"${uniqueNickname()}","regionCode":"$REGION_CODE"}"""),
    )

    private fun registerParticipant(): UUID =
        participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id

    private fun uploadCommand(participantId: UUID): UploadAnalysisCommand = UploadAnalysisCommand(
        participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, SELF_SPEAKER, SUBMITTING_IP,
    ) { TestChatFiles.kakaoIos(Instant.now(), EVENT_COUNT) }

    companion object {
        private const val REGISTERING_IP = "198.51.100.10"
        private const val OTHER_IP = "198.51.100.11"
        private const val SUBMITTING_IP = "198.51.100.20"
        private const val EVENT_COUNT = 20
    }
}
