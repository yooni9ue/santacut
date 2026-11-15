package com.santacut.presentation.controller

import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.command.AnalysisEventCommand
import com.santacut.application.command.AnalysisEventCommands
import com.santacut.application.command.RegisterParticipantCommand
import com.santacut.application.command.SubmitAnalysisCommand
import com.santacut.application.facade.AnalysisFacade
import com.santacut.application.facade.ParticipantFacade
import com.santacut.infrastructure.repository.AnalysisJobRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.test.assertIs

/**
 * 분석 이벤트 스트림이 폴링 중 오류를 만나면 스트림을 오류로 닫는지 확인한다.
 */
@IntegrationTest
class AnalysisStreamApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val analysisFacade: AnalysisFacade,
    private val analysisJobRepository: AnalysisJobRepository,
    private val clock: Clock,
) {
    @Test
    fun `스트림을 연 뒤 작업이 사라지면 폴링이 실패해 스트림을 오류로 닫는다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val jobId = submitAnalysis(participant.id)
        val stream = mockMvc.perform(
            get("/v1/analyses/$jobId/events").cookie(Cookie(SESSION_COOKIE, participant.sessionToken)),
        ).andExpect(request().asyncStarted()).andReturn()

        analysisJobRepository.deleteById(jobId)

        assertIs<IllegalStateException>(stream.getAsyncResult(ASYNC_WAIT_MILLIS))
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
        private const val ASYNC_WAIT_MILLIS = 10_000L
    }
}
