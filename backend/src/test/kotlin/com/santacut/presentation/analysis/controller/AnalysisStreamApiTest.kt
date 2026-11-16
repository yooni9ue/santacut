package com.santacut.presentation.analysis.controller

import com.santacut.MutableClock
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisEventCommand
import com.santacut.application.analysis.command.AnalysisEventCommands
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.analysis.facade.AnalysisWorkerFacade
import com.santacut.application.analysis.facade.WorkerTestClockConfiguration
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.service.AiBudgetService
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.prompt.repository.PromptRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 진행 상황 스트림(SSE)의 HTTP 계약을 확인한다. 연결하면 현재 단계를 받고, 작업자가 상태를 바꾸면 커밋 뒤 이벤트를 받으며,
 * 끝난 단계를 받으면 스트림이 끝난다. 인증·소유 규칙과 앱 전체 상한(여기서는 2개)도 확인한다.
 * 작업자가 대기열 전체를 보므로 `build/stream-test.db`를 따로 쓰고 테스트마다 비운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/stream-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.stream.max-open-streams=${AnalysisStreamApiTest.MAX_OPEN_STREAMS}",
    ],
)
@AutoConfigureMockMvc
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class AnalysisStreamApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val analysisFacade: AnalysisFacade,
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val aiUsageDayRepository: AiUsageDayRepository,
    private val promptRepository: PromptRepository,
    private val aiBudgetService: AiBudgetService,
) {
    /** 스트림 목록이 controller 하나에 있어 테스트 사이에 남지 않도록, 연 스트림을 테스트가 끝날 때 닫는다. */
    private val openedStreams = mutableListOf<MvcResult>()

    @AfterEach
    fun closeStreams() {
        openedStreams.forEach { runCatching { it.request.asyncContext?.complete() } }
    }

    @BeforeEach
    fun setUp() {
        analysisJobRepository.deleteAll()
        aiUsageDayRepository.deleteAll()
        promptRepository.deleteAll()
        promptRepository.save(Prompt.initial(PromptName.SCORE, PROMPT, OPEN_TIME))
        clock.moveTo(OPEN_TIME)
        aiBudgetService.recordSuccess()
        given(scoreClient.isEnabled()).willReturn(true)
        given(scoreClient.cacheKey(anyDigest(), anyInt())).willReturn(CACHE_KEY)
        given(scoreClient.requestAnswer(anyDigest(), anyString())).willReturn(fullAnswer())
    }

    @Test
    fun `연결하면 현재 단계를 받고, 작업이 바뀔 때마다 이벤트를 받으며, 끝난 단계를 받으면 스트림이 끝난다`() {
        val participant = register()
        val jobId = submit(participant.id)

        val stream = open(jobId, participant.session)
        assertTrue(stream.request.isAsyncStarted)
        assertTrue(hasEvent(stream.response.contentAsString, QUEUED_ID, "QUEUED"))

        analysisWorkerFacade.processNextJob()

        val body = stream.response.contentAsString
        assertTrue(hasEvent(body, ANALYZING_ID, "ANALYZING"), body)
        assertTrue(hasEvent(body, SUCCEEDED_ID, "SUCCEEDED"), body)
        assertTrue(body.contains("\"id\":\"$jobId\""), body)
        assertStreamCompleted(stream)
    }

    @Test
    fun `이미 끝난 작업에 다시 연결하면 끝난 단계를 받고 스트림이 바로 끝난다`() {
        val participant = register()
        val jobId = submit(participant.id)
        analysisWorkerFacade.processNextJob()

        val stream = mockMvc.perform(
            get(eventsPath(jobId)).cookie(participant.session).accept(MediaType.TEXT_EVENT_STREAM).header(LAST_EVENT_ID, "2"),
        ).andReturn()

        val body = stream.response.contentAsString
        assertTrue(hasEvent(body, SUCCEEDED_ID, "SUCCEEDED"), body)
        assertFalse(body.contains("QUEUED"), body)
        assertStreamCompleted(stream)
    }

    @Test
    fun `같은 참가자가 다시 열면 이전 스트림이 끝나고 새 스트림이 이벤트를 받는다`() {
        val participant = register()
        val jobId = submit(participant.id)
        val first = open(jobId, participant.session)

        val second = open(jobId, participant.session)

        assertStreamCompleted(first)
        assertTrue(hasEvent(second.response.contentAsString, QUEUED_ID, "QUEUED"))
    }

    @Test
    fun `세션 없이 열면 401과 NOT_AUTHENTICATED다`() {
        mockMvc.perform(get(eventsPath(UUID.randomUUID().toString())).accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"))
    }

    @Test
    fun `다른 참가자의 작업이나 없는 작업이면 404와 NOT_FOUND다`() {
        val owner = register()
        val jobId = submit(owner.id)
        val other = register()

        mockMvc.perform(get(eventsPath(jobId)).cookie(other.session).accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isNotFound)
            .andExpect(request().asyncNotStarted())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        mockMvc.perform(get(eventsPath(UUID.randomUUID().toString())).cookie(owner.session).accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `앱 전체 스트림이 상한에 닿으면 503과 ANALYSIS_STREAM_LIMIT, Retry-After를 준다`() {
        val opened = List(MAX_OPEN_STREAMS) { register() }.map { it to submit(it.id) }
        val streams = opened.map { (participant, jobId) -> open(jobId, participant.session) }
        val late = register()
        val lateJob = submit(late.id)

        mockMvc.perform(get(eventsPath(lateJob)).cookie(late.session).accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("ANALYSIS_STREAM_LIMIT"))
            .andExpect(jsonPath("$.retry_at").exists())
            .andExpect(header().string(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS))
        val (owner, ownerJob) = opened.first()
        assertTrue(open(ownerJob, owner.session).request.isAsyncStarted)
        streams.forEach { assertTrue(it.request.isAsyncStarted) }
    }

    private fun open(jobId: String, session: Cookie): MvcResult =
        mockMvc.perform(get(eventsPath(jobId)).cookie(session).accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(request().asyncStarted())
            .andReturn()
            .also { openedStreams.add(it) }

    /** 스트림이 끝나면 비동기 결과가 정해진다. */
    private fun assertStreamCompleted(stream: MvcResult) {
        stream.getAsyncResult(ASYNC_WAIT_MS)
    }

    /** Spring SseEmitter가 쓰는 `event:`, `id:`, `data:` 세 줄 가운데 data의 status가 맞는 이벤트가 있는지. */
    private fun hasEvent(body: String, eventId: Int, status: String): Boolean =
        Regex("event:$EVENT_NAME\nid:$eventId\ndata:\\{[^\n]*\"status\":\"$status\"").containsMatchIn(body)

    private fun register(): StreamParticipant {
        val registered = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        return StreamParticipant(registered.id, Cookie(SESSION_COOKIE, registered.sessionToken))
    }

    private fun submit(participantId: UUID): String {
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt.minus(Duration.ofHours(EVENT_SPACING_HOURS * index)), "$SAMPLE_TEXT $index") })
        return analysisFacade.submit(SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), SOURCE, events)).id
    }

    private fun eventsPath(jobId: String): String = "/v1/analyses/$jobId/events"

    private fun fullAnswer(): ScoreAnswer =
        ScoreAnswer.of(List(DIMENSION_COUNT) { BigDecimal(SCORE) }, List(DIMENSION_COUNT) { listOf(1) }, "test", USAGE)

    private fun anyDigest(): ScoringDigest =
        any(ScoringDigest::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).toScoringDigest()

    companion object {
        const val MAX_OPEN_STREAMS = 2
        private const val RETRY_AFTER_SECONDS = "30"
        private const val SESSION_COOKIE = "sc_session"
        private const val LAST_EVENT_ID = "Last-Event-ID"
        private const val EVENT_NAME = "status"
        private const val QUEUED_ID = 1
        private const val ANALYZING_ID = 2
        private const val SUCCEEDED_ID = 4
        private const val ASYNC_WAIT_MS = 2_000L
        private const val SOURCE = "KAKAO"
        private const val EVENT_COUNT = 20

        /** 대화문에서 한 줄로 이어지지 않게 이벤트 사이를 3시간보다 넓게 띄운다. */
        private const val EVENT_SPACING_HOURS = 4L
        private const val SAMPLE_TEXT = "오늘 정말 고마웠어 덕분에 힘이 났어"
        private const val CACHE_KEY = "같은 채점 입력"
        private const val PROMPT = "채점 프롬프트"
        private const val DIMENSION_COUNT = 4
        private const val SCORE = 80
        private val USAGE = ModelUsage(4_000, 90, "end_turn")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
