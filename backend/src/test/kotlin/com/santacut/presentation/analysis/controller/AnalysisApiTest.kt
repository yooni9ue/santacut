package com.santacut.presentation.analysis.controller

import com.santacut.IntegrationTest
import com.santacut.TestCsrf.withCsrf
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisEventCommand
import com.santacut.application.analysis.command.AnalysisEventCommands
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * 분석 제출과 상태 폴링 경로의 HTTP 계약(상태 코드, 오류 코드, 헤더)을 확인한다.
 */
@IntegrationTest
class AnalysisApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val analysisFacade: AnalysisFacade,
    private val clock: Clock,
) {
    private val occurredAt = clock.instant().minus(Duration.ofDays(1))

    @Test
    fun `제출한 작업의 진행 단계를 작업 ID와 내 최근 작업 경로로 조회한다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val jobId = submitAnalysis(participant.id)
        val session = Cookie(SESSION_COOKIE, participant.sessionToken)

        mockMvc.perform(get("/v1/analyses/$jobId").cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(jobId))
            .andExpect(jsonPath("$.status").value("QUEUED"))
        mockMvc.perform(get("/v1/me/analysis").cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(jobId))
    }

    @Test
    fun `다른 참가자의 작업이나 낸 작업이 없으면 404와 NOT_FOUND다`() {
        val owner = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val jobId = submitAnalysis(owner.id)
        val other = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val session = Cookie(SESSION_COOKIE, other.sessionToken)

        mockMvc.perform(get("/v1/analyses/$jobId").cookie(session))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        mockMvc.perform(get("/v1/me/analysis").cookie(session))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `세션 없이 상태를 조회하면 401과 NOT_AUTHENTICATED다`() {
        mockMvc.perform(get("/v1/analyses/${UUID.randomUUID()}"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"))
        mockMvc.perform(get("/v1/me/analysis"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"))
            .andExpect(jsonPath("$.path").value("/v1/me/analysis"))
    }

    @Test
    fun `형식이 틀린 요청은 400과 INVALID_REQUEST다`() {
        val session = registerSession()
        mockMvc.perform(submitRequest(session, events(EVENT_COUNT)).header(IDEMPOTENCY_KEY, newKey()).content("{"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(submitRequest(session, events(EVENT_COUNT)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(get("/v1/analyses/not-a-uuid").cookie(session))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `이벤트가 20개보다 적으면 400과 INSUFFICIENT_EVENTS다`() {
        val session = registerSession()
        mockMvc.perform(submitRequest(session, events(EVENT_COUNT - 1)).header(IDEMPOTENCY_KEY, newKey()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_EVENTS"))
    }

    @Test
    fun `하루 제출 한도를 넘으면 429와 다시 낼 수 있는 시각을 본문과 Retry-After 헤더로 준다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        repeat(MAX_DAILY_SUBMISSIONS) { submitAnalysis(participant.id) }
        val session = Cookie(SESSION_COOKIE, participant.sessionToken)

        mockMvc.perform(submitRequest(session, events(EVENT_COUNT)).header(IDEMPOTENCY_KEY, newKey()))
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("DAILY_USER_LIMIT"))
            .andExpect(jsonPath("$.retry_at").isString)
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
    }

    private fun registerSession(): Cookie {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        return Cookie(SESSION_COOKIE, participant.sessionToken)
    }

    private fun submitRequest(session: Cookie, body: String): MockHttpServletRequestBuilder =
        withCsrf(mockMvc, post("/v1/analyses"))
            .cookie(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

    private fun events(count: Int): String {
        val event = "{\"occurredAt\":\"$occurredAt\",\"text\":\"고마워\"}"
        return "{\"source\":\"$SOURCE\",\"events\":[" + List(count) { event }.joinToString(",") + "]}"
    }

    private fun submitAnalysis(participantId: UUID): String {
        val events = AnalysisEventCommands(
            List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt, "고마워 $index") },
        )
        val command = SubmitAnalysisCommand(participantId, newKey(), SOURCE, events)
        return analysisFacade.submit(command).id
    }

    private fun newKey(): String = "key-" + UUID.randomUUID()

    companion object {
        private const val SESSION_COOKIE = "sc_session"
        private const val IDEMPOTENCY_KEY = "Idempotency-Key"
        private const val SOURCE = "KAKAO"
        private const val EVENT_COUNT = 20
        private const val MAX_DAILY_SUBMISSIONS = 3
    }
}
