package com.santacut.application.analysis.facade

import com.santacut.MutableClock
import com.santacut.TestDossiers
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisEventCommand
import com.santacut.application.analysis.command.AnalysisEventCommands
import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.result.AnalysisStatusResult
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.facade.RankingRebuildFacade
import com.santacut.application.ranking.facade.ResultFacade
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.TranscriptTurns
import com.santacut.domain.analysis.service.AiBudgetService
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.roast.model.RoastAnswer
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.roast.client.RoastClient
import com.santacut.infrastructure.roast.repository.RoastRepository
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.analysis.repository.AssessmentRepository
import com.santacut.infrastructure.dossier.repository.DossierRepository
import com.santacut.infrastructure.prompt.repository.PromptRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 분석 작업자가 AI 점수 클라이언트를 바꿔 끼운 상태에서 성공, 실패, 연속 처리, 시즌 마감을 처리하는지 확인한다.
 * AI 호출마다 DB의 최신 채점 프롬프트를 쓰는지, 같은 채점 입력의 재제출에 지난 결과를 다시 쓰는지,
 * 호출 전 차단, 제공자 차단, 앱 안의 비용 가드도 확인한다.
 * 대기열 전체를 보는 작업자라 다른 통합 테스트와 섞이지 않게 `build/worker-test.db`를 따로 쓰고 테스트마다 비운다.
 * 작업자를 멈추게 하는 테스트는 멈춤 상태가 앱 메모리에 남으므로 끝난 뒤 컨텍스트를 새로 띄운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-test.db",
        "app.season-year=${AnalysisWorkerFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        // 시작 직후 예약 폴링이 목을 부르면 이 스레드의 스텁이 다른 메서드에 붙는다. 작업자는 테스트가 직접 부른다.
        "santacut.scheduling.enabled=false",
        // 호출 수와 사용량을 세는 검증은 한 번 묻는 예전 흐름 기준이다. 중앙값 반복은 ScoringChunksTest·ScoreAnswersTest가 본다.
        "santacut.score.sampling.samples=1",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class, RoastClient::class])
@MockitoSpyBean(types = [DossierService::class])
class AnalysisWorkerFacadeTest @Autowired constructor(
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val analysisStatusFacade: AnalysisStatusFacade,
    private val rankingRebuildFacade: RankingRebuildFacade,
    private val resultFacade: ResultFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val roastClient: RoastClient,
    private val roastRepository: RoastRepository,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val promptRepository: PromptRepository,
    private val assessmentRepository: AssessmentRepository,
    private val aiUsageDayRepository: AiUsageDayRepository,
    private val aiBudgetService: AiBudgetService,
    private val dossierRepository: DossierRepository,
    private val dossierService: DossierService,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val seasonClose: Instant = Season(SEASON_YEAR).deliveryStart().toInstant()

    @BeforeEach
    fun setUp() {
        analysisJobRepository.deleteAll()
        aiUsageDayRepository.deleteAll()
        promptRepository.deleteAll()
        promptRepository.save(Prompt.initial(PromptName.SCORE, FIRST_PROMPT, OPEN_TIME))
        clock.moveTo(OPEN_TIME)
        aiBudgetService.recordSuccess()
        given(scoreClient.isEnabled()).willReturn(true)
        given(scoreClient.cacheKey(anyDigest(), anyInt())).willReturn(CACHE_KEY)
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(ESTIMATE)
    }

    @Test
    fun `점수를 받으면 평가를 저장하고 순위를 다시 계산해 작업을 성공으로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("SUCCEEDED", status.status)
        assertNotNull(status.assessmentId)
        val outcome = resultFacade.getMyResult(MyResultCommand(job.participantId))
        assertEquals(EXPECTED_SCORE_HUNDREDTHS, outcome.scoreHundredths)
        assertEquals(1, outcome.rank)
    }

    @Test
    fun `점수 산정 중 예외가 나면 작업을 ANALYSIS_FAILED로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willThrow(IllegalStateException("AI 장애"))
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("ANALYSIS_FAILED", status.errorCode)
        assertNull(status.assessmentId)
        assertNotNull(status.finishedAt)
    }

    @Test
    fun `여러 작업을 연달아 채점한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val jobs = List(CONSECUTIVE_JOB_COUNT) { submitAnalysis() }

        repeat(CONSECUTIVE_JOB_COUNT) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(CONSECUTIVE_JOB_COUNT) { "SUCCEEDED" }, jobs.map { rankedStatus(it).status })
    }

    @Test
    fun `AI가 꺼져 있어도 24시간 넘게 기다린 작업은 EXPIRED로 끝내고 AI는 부르지 않는다`() {
        val job = submitAnalysis()
        given(scoreClient.isEnabled()).willReturn(false)
        clock.moveTo(OPEN_TIME.plus(AnalysisJob.QUEUE_EXPIRY).plusSeconds(1))

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("EXPIRED", status.errorCode)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `AI가 꺼져 있어도 리스가 끝난 실행 작업은 대기열로 되돌리고 24시간이 지나면 만료하며 제출 원문을 지운다`() {
        val job = submitAnalysis()
        abandonAttempt(job)
        given(scoreClient.isEnabled()).willReturn(false)
        clock.moveTo(OPEN_TIME.plus(ABANDONED_LEASE).plus(ABANDONED_LEASE))

        analysisWorkerFacade.processNextJob()

        assertEquals("QUEUED", rankedStatus(job).status)
        clock.moveTo(OPEN_TIME.plus(AnalysisJob.QUEUE_EXPIRY).plusSeconds(1))
        analysisWorkerFacade.processNextJob()
        val expired = storedJob(job)
        assertEquals(AnalysisFailureCode.EXPIRED, expired.errorCode)
        assertNull(expired.payload)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `리스가 끝난 실행 작업은 뒤에 들어온 대기 작업보다 먼저 다시 잡히고 끝난 시도도 센다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val abandoned = submitAnalysis()
        abandonAttempt(abandoned)
        clock.moveTo(OPEN_TIME.plus(ABANDONED_LEASE).plus(ABANDONED_LEASE))
        val later = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("SUCCEEDED", rankedStatus(abandoned).status)
        assertEquals(2, storedJob(abandoned).attempts)
        assertEquals("QUEUED", rankedStatus(later).status)
    }

    @Test
    fun `점수를 받은 사이에 시즌이 마감되면 평가를 저장하지 않고 SEASON_FROZEN으로 끝낸다`() {
        clock.moveTo(seasonClose.minus(Duration.ofMinutes(1)))
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            clock.moveTo(seasonClose)
            fullAnswer()
        }
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("SEASON_FROZEN", status.errorCode)
        assertNull(status.assessmentId)
    }

    @Test
    fun `시즌 마감 뒤 밀린 작업은 잡을 때 AI를 부르지 않고 모두 SEASON_FROZEN으로 끝내며 제출 원문을 지운다`() {
        clock.moveTo(seasonClose.minus(Duration.ofMinutes(1)))
        val jobs = List(2) { submitAnalysis() }
        clock.moveTo(seasonClose)

        analysisWorkerFacade.processNextJob()

        assertEquals(List(jobs.size) { "SEASON_FROZEN" }, jobs.map { rankedStatus(it).errorCode })
        assertEquals(List(jobs.size) { null }, jobs.map { storedJob(it).payload })
        verify(scoreClient, never()).estimateUsage(anyChunk(), anyString())
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `AI를 부를 때마다 채점 프롬프트의 최신 판을 보내고 평가에 그 판 번호를 남긴다`() {
        val first = checkNotNull(promptRepository.findFirstByNameOrderByVersionDesc(PromptName.SCORE.value))
        val latest = promptRepository.save(first.next(LATEST_PROMPT, OPEN_TIME))
        given(scoreClient.requestAnswer(anyChunk(), eq(LATEST_PROMPT) ?: LATEST_PROMPT)).willReturn(fullAnswer())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("SUCCEEDED", rankedStatus(job).status)
        val assessment = assessmentRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(job.participantId.toString())
        assertEquals(latest.version, assessment?.promptVersion)
    }

    @Test
    fun `채점 프롬프트가 하나도 없으면 AI를 부르지 않고 작업을 ANALYSIS_FAILED로 끝낸다`() {
        promptRepository.deleteAll()
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("ANALYSIS_FAILED", status.errorCode)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `표본이 너무 짧으면 AI를 부르지 않고 근거 부족으로 끝낸다`() {
        val job = submitAnalysis(text = SHORT_TEXT)

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("INSUFFICIENT_EVIDENCE", status.errorCode)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `같은 참가자가 같은 채점 입력으로 다시 내면 AI를 다시 부르지 않고 지난 점수를 새 평가로 옮겨 적는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(second)
        assertEquals("SUCCEEDED", status.status)
        assertNotEquals(rankedStatus(first).assessmentId, status.assessmentId)
        assertEquals(EXPECTED_SCORE_HUNDREDTHS, resultFacade.getMyResult(MyResultCommand(second.participantId)).scoreHundredths)
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `채점을 마치면 같은 트랜잭션에서 그 평가의 조서 집계를 저장하고 제출 원문은 지운다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val assessmentId = assertNotNull(rankedStatus(job).assessmentId)
        val dossier = assertNotNull(dossierRepository.findByAssessmentId(assessmentId))
        assertTrue(dossier.statsJson.contains("\"message_count\":$EVENT_COUNT"))
        assertFalse(dossier.statsJson.contains(SAMPLE_TEXT))
        assertNull(storedJob(job).payload)
    }

    @Test
    fun `같은 채점 입력으로 다시 내면 지난 평가의 조서 집계를 새 평가에 복사한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val previous = assertNotNull(dossierRepository.findByAssessmentId(assertNotNull(rankedStatus(first).assessmentId)))
        val copied = assertNotNull(dossierRepository.findByAssessmentId(assertNotNull(rankedStatus(second).assessmentId)))
        assertNotEquals(previous.id, copied.id)
        assertEquals(previous.statsJson, copied.statsJson)
        assertEquals(previous.tone, copied.tone)
    }

    @Test
    fun `지난 평가에 조서가 없으면 같은 채점 입력을 다시 내도 이번 작업으로 미리 센 조서를 저장한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        dossierRepository.deleteAll()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val recounted = assertNotNull(dossierRepository.findByAssessmentId(assertNotNull(rankedStatus(second).assessmentId)))
        assertTrue(recounted.statsJson.contains("\"message_count\":$EVENT_COUNT"))
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `지난 조서의 집계 규칙 판이 다르면 복사하지 않고 이번 작업으로 미리 센 조서를 저장한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        jdbcTemplate.update("UPDATE dossiers SET stats_version = ?, stats_json = '{}'", DossierStats.VERSION - 1)
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val recounted = assertNotNull(dossierRepository.findByAssessmentId(assertNotNull(rankedStatus(second).assessmentId)))
        assertEquals(DossierStats.VERSION, recounted.statsVersion)
        assertTrue(recounted.statsJson.contains("\"message_count\":$EVENT_COUNT"))
    }

    @Test
    fun `원본 파일 정규화 때 분석 기간 전체로 센 조서 집계가 페이로드에 있으면 채점 표본 대신 그 값을 저장한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val job = submitAnalysis()
        attachDossier(job, fullPeriodTally(DossierStats.VERSION))

        analysisWorkerFacade.processNextJob()

        val dossier = assertNotNull(dossierRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        assertTrue(dossier.statsJson.contains("\"message_count\":$FULL_PERIOD_COUNT"))
    }

    @Test
    fun `페이로드의 전체 기간 집계가 예전 집계 규칙 판이면 버리고 제출 이벤트로 다시 센다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val job = submitAnalysis()
        attachDossier(job, fullPeriodTally(DossierStats.VERSION - 1))

        analysisWorkerFacade.processNextJob()

        val dossier = assertNotNull(dossierRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        assertTrue(dossier.statsJson.contains("\"message_count\":$EVENT_COUNT"))
    }

    @Test
    fun `조서 집계에 실패해도 조서 없이 평가를 저장하고 작업을 성공으로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        doReturn(null).`when`(dossierService).draft(anyJob())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = rankedStatus(job)
        assertEquals("SUCCEEDED", status.status)
        assertNull(dossierRepository.findByAssessmentId(assertNotNull(status.assessmentId)))
    }

    @Test
    fun `같은 채점 입력이 근거 부족으로 끝났으면 다시 내도 AI 없이 근거 부족으로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(ScoreAnswer.insufficient(USAGE))
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        assertEquals("INSUFFICIENT_EVIDENCE", rankedStatus(first).errorCode)
        assertEquals("INSUFFICIENT_EVIDENCE", rankedStatus(second).errorCode)
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `다른 참가자의 같은 채점 입력은 다시 쓰지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        submitAnalysis()
        submitAnalysis()

        repeat(2) { analysisWorkerFacade.processNextJob() }

        verify(scoreClient, times(2)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `제공자가 호출을 막으면 작업을 실패시키지 않고 대기열로 되돌린 뒤 멈춘다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(ScoreAnswer.providerUnavailable())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()
        analysisWorkerFacade.processNextJob()

        assertEquals("QUEUED", rankedStatus(job).status)
        assertEquals(0, storedJob(job).attempts)
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `앞 구간을 과금한 뒤 제공자가 막으면 시도를 세어 되돌리고 시도를 다 쓰면 RETRY_EXHAUSTED로 끝낸다`() {
        val calls = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            if (calls.getAndIncrement() % CHUNK_COUNT == 0) fullAnswer() else ScoreAnswer.providerUnavailable()
        }
        val job = submitAnalysis(text = LONG_TEXT, eventCount = LONG_EVENT_COUNT)

        repeat(AnalysisJob.MAX_ATTEMPTS) { round ->
            clock.moveTo(OPEN_TIME.plus(PAUSE_PASSED.multipliedBy(round.toLong())))
            analysisWorkerFacade.processNextJob()
        }

        val exhausted = storedJob(job)
        assertEquals(AnalysisFailureCode.RETRY_EXHAUSTED, exhausted.errorCode)
        assertNull(exhausted.payload)
        verify(scoreClient, times(AnalysisJob.MAX_ATTEMPTS * CHUNK_COUNT)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `분석 오류가 3번 연달아 나면 멈춰서 다음 작업을 잡지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willThrow(IllegalStateException("AI 장애"))
        val jobs = List(MAX_CONSECUTIVE_FAILURES + 1) { submitAnalysis() }

        repeat(jobs.size) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(MAX_CONSECUTIVE_FAILURES) { "ANALYSIS_FAILED" }, jobs.dropLast(1).map { rankedStatus(it).errorCode })
        assertEquals("QUEUED", rankedStatus(jobs.last()).status)
    }

    @Test
    fun `근거 부족은 연달아 나도 멈추지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(ScoreAnswer.insufficient(USAGE))
        val jobs = List(MAX_CONSECUTIVE_FAILURES + 1) { submitAnalysis() }

        repeat(jobs.size) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(jobs.size) { "INSUFFICIENT_EVIDENCE" }, jobs.map { rankedStatus(it).errorCode })
    }

    @Test
    fun `입력 탓 응답 오류는 연달아 나도 멈추지 않고 실패로 끝낸다`() {
        val outOfRange = ScoreAnswer.of(List(DIMENSION_COUNT) { OUT_OF_RANGE_SCORE }, List(DIMENSION_COUNT) { listOf(1) }, "test", USAGE)
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(outOfRange)
        val jobs = List(MAX_CONSECUTIVE_FAILURES + 1) { submitAnalysis() }

        repeat(jobs.size) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(jobs.size) { "ANALYSIS_FAILED" }, jobs.map { rankedStatus(it).errorCode })
        assertFalse(aiBudgetService.isBlocked(clock.instant()))
    }

    @Test
    fun `한 달 비용 한도에 닿으면 다음 작업을 잡지 않고 대기열에 둔다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer(EXPENSIVE_USAGE))
        val first = submitAnalysis()
        val second = submitAnalysis()

        repeat(2) { analysisWorkerFacade.processNextJob() }

        assertEquals("SUCCEEDED", rankedStatus(first).status)
        assertEquals("QUEUED", rankedStatus(second).status)
        assertEquals(EXPENSIVE_USAGE.inputTokens, aiUsageDayRepository.findAll().single().inputTokens)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `호출 전 어림 비용을 더하면 한도를 넘을 때는 AI를 부르지 않고 대기열로 되돌린 뒤 멈춘다`() {
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(OVER_BUDGET_ESTIMATE)
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("QUEUED", rankedStatus(job).status)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
        assertTrue(aiBudgetService.isBlocked(clock.instant()))
    }

    @Test
    fun `채점을 마치면 작가 문장을 검증해 통과한 문장을 팩폭 결과로 평가와 함께 저장한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        enableRoast()
        given(roastClient.request(anyString(), anyString())).willReturn(RoastAnswer(roastJson(VALID_TITLE), ROAST_USAGE))
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val roast = assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        assertEquals(1, roast.promptVersion)
        assertEquals(ROAST_MODEL, roast.model)
        assertTrue(roast.contentJson.contains("\"text\":\"$VALID_TITLE\",\"source\":\"AI\""))
        assertFalse(roast.contentJson.contains(SAMPLE_TEXT))
        assertNull(storedJob(job).payload)
    }

    @Test
    fun `검증에 떨어진 필드는 사유를 붙여 최대 두 번 다시 부르고 끝까지 떨어지면 대체 문장으로 채운다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        enableRoast()
        val inputs = mutableListOf<String>()
        given(roastClient.request(anyString(), anyString())).willAnswer { invocation ->
            inputs += invocation.getArgument<String>(1)
            RoastAnswer(roastJson("물음표 세 번 요정"), ROAST_USAGE)
        }
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals(3, inputs.size)
        assertFalse(inputs[0].contains("\"rewrite\""))
        assertTrue(inputs[1].contains("\"rewrite\"") && inputs[1].contains("title 숫자 표현(세 번)"))
        val roast = assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        assertFalse(roast.contentJson.contains("물음표 세 번 요정"))
        assertEquals("SUCCEEDED", rankedStatus(job).status)
    }

    @Test
    fun `작가 호출이 예외를 던져도 채점 결과는 정상 저장되고 팩폭은 대체 문장으로 채운다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        enableRoast()
        given(roastClient.request(anyString(), anyString())).willThrow(IllegalStateException("작가 장애"))
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("SUCCEEDED", rankedStatus(job).status)
        val roast = assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        assertNull(roast.promptVersion)
        assertEquals("", roast.model)
        assertTrue(roast.contentJson.contains("\"source\":\"FALLBACK\""))
    }

    @Test
    fun `작가 비용 예약이 한도에 막혀도 채점 결과는 저장되고 작가는 부르지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        enableRoast()
        given(roastClient.estimateUsage(anyString(), anyString())).willReturn(OVER_BUDGET_ESTIMATE)
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("SUCCEEDED", rankedStatus(job).status)
        assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        verify(roastClient, never()).request(anyString(), anyString())
    }

    @Test
    fun `작가 프롬프트가 없으면 작가를 부르지 않고 대체 문장으로 저장한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        enableRoast(withPrompt = false)
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(job).assessmentId)))
        verify(roastClient, never()).request(anyString(), anyString())
    }

    @Test
    fun `같은 채점 입력으로 다시 내면 지난 팩폭 결과를 새 평가에 복사하고 작가를 다시 부르지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        enableRoast()
        given(roastClient.request(anyString(), anyString())).willReturn(RoastAnswer(roastJson(VALID_TITLE), ROAST_USAGE))
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val calls = requestCount()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val previous = assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(first).assessmentId)))
        val copied = assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(second).assessmentId)))
        assertNotEquals(previous.id, copied.id)
        assertEquals(previous.contentJson, copied.contentJson)
        assertEquals(calls, requestCount())
    }

    @Test
    fun `지난 평가에 팩폭 결과가 없으면 같은 입력 재사용 때 작가 없이 대체 문장으로 저장한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        roastRepository.deleteAll()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val saved = assertNotNull(roastRepository.findByAssessmentId(assertNotNull(rankedStatus(second).assessmentId)))
        assertNull(saved.promptVersion)
        assertTrue(saved.contentJson.contains("\"verdict\":\"gift\""))
    }

    /** 작가를 켜고 작가 프롬프트 1판을 넣는다([withPrompt]가 거짓이면 넣지 않는다). */
    private fun enableRoast(withPrompt: Boolean = true) {
        given(roastClient.isEnabled()).willReturn(true)
        given(roastClient.model()).willReturn(ROAST_MODEL)
        given(roastClient.estimateUsage(anyString(), anyString())).willReturn(ESTIMATE)
        if (withPrompt) promptRepository.save(Prompt.initial(PromptName.ROAST, ROAST_PROMPT, OPEN_TIME))
    }

    private fun requestCount(): Int = mockingDetails(roastClient).invocations.count { invocation -> invocation.method.name == "request" }

    /** 칭호가 [title]이고 나머지는 규칙을 지킨 작가 응답. 짝이 없으므로 짝은 대체 문장으로 채워진다. */
    private fun roastJson(title: String): String = objectMapper.writeValueAsString(
        mapOf(
            "title" to title,
            "verdict" to "말풍선마다 고맙다는 말을 얹었다. 선물 명단에 이름 적어 둔다.",
            "verdict_mild" to "올해 말풍선 잘 읽었다. 선물은 문 앞에 잘 챙겨 두고 간다.",
            "letter" to "올해 네 말풍선을 끝까지 읽었다. 할 말이 생기면 미루지 않고 먼저 꺼내던 버릇이 좋았다. 내년에도 그대로 두어라.",
            "pairs" to emptyList<Any>(),
            "story" to listOf(1, 2, 3).map { act -> mapOf("act" to act, "line" to "그 막에는 고맙다는 말을 먼저 꺼냈다.") },
        ),
    )


    private fun submitAnalysis(
        participantId: UUID = registerParticipant(),
        text: String = SAMPLE_TEXT,
        eventCount: Int = EVENT_COUNT,
    ): AnalysisJobCommand {
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(List(eventCount) { index -> AnalysisEventCommand(occurredAt.minus(Duration.ofHours(EVENT_SPACING_HOURS * index)), "$text $index") })
        val command = SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, events)
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.submit(command).id))
    }

    /** 작업자가 작업을 잡은 채 죽은 상태를 만든다. 리스는 [ABANDONED_LEASE] 뒤에 끝난다. */
    private fun abandonAttempt(job: AnalysisJobCommand) {
        val stored = storedJob(job)
        stored.startAttempt(ABANDONED_LEASE_TOKEN, clock.instant(), ABANDONED_LEASE)
        analysisJobRepository.save(stored)
    }

    private fun storedJob(job: AnalysisJobCommand): AnalysisJob = analysisJobRepository.findById(job.jobId.toString()).orElseThrow()

    /** 원본 파일 검증 작업자가 담는 것처럼 작업 페이로드에 조서 집계 [tally]를 더한다. */
    private fun attachDossier(job: AnalysisJobCommand, tally: DossierTally) {
        val submission = objectMapper.readValue(storedJob(job).requirePayload(), AnalysisSubmission::class.java)
        jdbcTemplate.update(
            "UPDATE analysis_jobs SET payload = ? WHERE id = ?", objectMapper.writeValueAsString(submission.withDossier(tally)), job.jobId.toString(),
        )
    }

    /** 본인 메시지가 [FULL_PERIOD_COUNT]개였던 원본 전체로 [version] 판 규칙으로 센 집계. */
    private fun fullPeriodTally(version: Int): DossierTally {
        val tally = TestDossiers.tally(TestDossiers.selves(listOf("고마워")))
        val volume = tally.stats.volume.copy(messageCount = FULL_PERIOD_COUNT, textCount = FULL_PERIOD_COUNT)
        return tally.copy(stats = tally.stats.copy(version = version, volume = volume))
    }

    private fun registerParticipant(): UUID =
        participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id

    /**
     * 모든 항목 100점, 근거는 첫 표본 줄. 기회 수가 없는 예전 형식이라 n=2로 보고,
     * 본인 턴이 20개 이상이라 근거 무게 w=2다. C=0.5로 수축하면 항목마다 (0.5×55 + 2×100) / 2.5 = 91점, 합계 9,100이다.
     */
    private fun fullAnswer(usage: ModelUsage = USAGE): ScoreAnswer =
        ScoreAnswer.of(List(DIMENSION_COUNT) { DIMENSION_SCORE }, List(DIMENSION_COUNT) { listOf(1) }, "test", usage)

    /** Kotlin의 null 불가 매개변수에도 쓸 수 있도록 매처를 등록한 뒤 null 대신 빈 제출의 채점 입력을 돌려준다. */
    private fun anyDigest(): ScoringDigest =
        any(ScoringDigest::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).toScoringDigest()

    /** 매처를 등록한 뒤 null 대신 빈 제출의 작업을 돌려준다. */
    private fun anyJob(): AnalysisJob = any(AnalysisJob::class.java) ?: AnalysisJob.enqueue(
        UUID.randomUUID(), IdempotencyKey.from("key-any-analysis-job"), AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
        AnalysisPayload("{}"), OPEN_TIME,
    )

    /** 매처를 등록한 뒤 null 대신 빈 제출 전체를 한 구간으로 돌려준다. */
    private fun anyChunk(): ScoringChunk = any(ScoringChunk::class.java) ?: ScoringChunk(TranscriptTurns(emptyList()), 1, 0, 0)

    /** 쌓인 순위 다시 계산 요청을 처리한 뒤 작업 상태를 읽는다. 성공한 작업은 순위에 반영돼야 SUCCEEDED다. */
    private fun rankedStatus(command: AnalysisJobCommand): AnalysisStatusResult {
        rankingRebuildFacade.rebuildIfRequested()
        return analysisStatusFacade.getStatus(command)
    }

    companion object {
        const val SEASON_YEAR = 2099
        private const val CONSECUTIVE_JOB_COUNT = 6
        private const val EVENT_COUNT = 20

        /** 채점 표본(이벤트 [EVENT_COUNT]개)보다 많은, 원본 파일 전체의 본인 메시지 수. */
        private const val FULL_PERIOD_COUNT = 9999

        /** 대화문에서 한 줄로 이어지지 않게 이벤트 사이를 3시간보다 넓게 띄운다. */
        private const val EVENT_SPACING_HOURS = 4L
        private const val SAMPLE_TEXT = "오늘 정말 고마웠어 덕분에 힘이 났어"
        private const val SHORT_TEXT = "고마워"
        private const val CACHE_KEY = "같은 채점 입력"
        private const val FIRST_PROMPT = "채점 프롬프트 1판"
        private const val ROAST_PROMPT = "작가 프롬프트 1판"
        private const val ROAST_MODEL = "gemini-3.5-flash"
        private const val VALID_TITLE = "고맙다 연타 요정"
        private val ROAST_USAGE = ModelUsage(5_000, 400, "STOP")
        private const val LATEST_PROMPT = "채점 프롬프트 2판"
        private const val DIMENSION_COUNT = 4
        private const val EXPECTED_SCORE_HUNDREDTHS = 9500
        private val DIMENSION_SCORE = BigDecimal("100")
        private val OUT_OF_RANGE_SCORE = BigDecimal("101")
        private val USAGE = ModelUsage(4_000, 90, "end_turn")

        /** 입력 60만 토큰. 단가(100만 토큰당 1달러)로 0.6달러라 하루 한도 0.5달러를 넘는다. */
        /** 입력 단가 0.3달러 × 3,000만 토큰 = 한 달 한도 9달러. */
        private val EXPENSIVE_USAGE = ModelUsage(30_000_000, 0, "end_turn")
        private val ESTIMATE = ModelUsage(40_000, 250, "ESTIMATE")

        /** 한 번만으로 한 달 한도(9달러)를 넘는 어림 사용량. */
        private val OVER_BUDGET_ESTIMATE = ModelUsage(31_000_000, 250, "ESTIMATE")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")

        private const val MAX_CONSECUTIVE_FAILURES = 3
        /** 메시지 하나의 정규화 상한(300자) 안에서 긴 글. 이벤트 30개면 약 8,500자라 구간 목표(6,700자)를 넘어 두 구간으로 나뉜다. */
        private val LONG_TEXT = SAMPLE_TEXT.repeat(14)
        private const val LONG_EVENT_COUNT = 30
        private const val CHUNK_COUNT = 2

        /** 제공자 차단 멈춤(15분)이 지난 시간. */
        private val PAUSE_PASSED: Duration = Duration.ofMinutes(16)
        private const val ABANDONED_LEASE_TOKEN = "죽은 작업자의 리스"
        private val ABANDONED_LEASE: Duration = Duration.ofMinutes(10)
    }
}
