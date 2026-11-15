package com.santacut.presentation.analysis.controller

import com.santacut.IntegrationTest
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * 분석 상태 폴링 경로(작업 하나, 내 최근 작업)의 HTTP 계약을 확인한다.
 */
@IntegrationTest
class AnalysisApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val analysisFacade: AnalysisFacade,
    private val clock: Clock,
) {
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
    fun `세션 없이 상태를 조회하면 401이다`() {
        mockMvc.perform(get("/v1/analyses/${UUID.randomUUID()}")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/me/analysis")).andExpect(status().isUnauthorized)
    }

    private fun submitAnalysis(participantId: UUID): String {
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(
            List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt, "고마워 $index") },
        )
        val command = SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), SOURCE, events)
        return analysisFacade.submit(command).id
    }

    companion object {
        private const val SESSION_COOKIE = "sc_session"
        private const val SOURCE = "KAKAO"
        private const val EVENT_COUNT = 20
    }
}
