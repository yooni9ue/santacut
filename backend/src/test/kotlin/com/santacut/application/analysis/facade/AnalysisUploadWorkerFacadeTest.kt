package com.santacut.application.analysis.facade

import com.santacut.MutableClock
import com.santacut.TestChatFiles
import com.santacut.TestChatFiles.OTHER_SPEAKER
import com.santacut.TestChatFiles.OTHER_TEXT
import com.santacut.TestChatFiles.SELF_SPEAKER
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.UploadAnalysisCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.AnalysisUpload
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.service.AnalysisUploadService
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.analysis.repository.AnalysisUploadRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration
import java.time.Instant
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 원본 파일 검증 작업자가 성공·실패·만료·시도 소진을 처리하고 어느 쪽이든 원본을 지우는지 확인한다.
 * 분석 작업자 테스트와 같은 `build/worker-test.db`를 쓰고 테스트마다 비운다. 예상하지 못한 예외를 내려고
 * 원본 서비스를 스파이로 바꾸므로 컨텍스트는 따로 뜬다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-test.db",
        "app.season-year=${AnalysisWorkerFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
@MockitoSpyBean(types = [AnalysisUploadService::class])
class AnalysisUploadWorkerFacadeTest @Autowired constructor(
    private val analysisUploadWorkerFacade: AnalysisUploadWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val analysisStatusFacade: AnalysisStatusFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val analysisUploadRepository: AnalysisUploadRepository,
    private val analysisUploadService: AnalysisUploadService,
    private val objectMapper: ObjectMapper,
) {
    @BeforeEach
    fun setUp() {
        analysisUploadRepository.deleteAll()
        analysisJobRepository.deleteAll()
        clock.moveTo(OPEN_TIME)
        given(scoreClient.isEnabled()).willReturn(true)
    }

    /** 상대 이름은 이벤트(본문·화자 표시)에는 없고, 팩폭 작가가 이름을 가리는 데 쓰는 참여자 이름 목록에만 있다. */
    private fun assertSpeakerOnlyInNames(payload: String) {
        val submission = objectMapper.readValue(payload, AnalysisSubmission::class.java)
        assertFalse(objectMapper.writeValueAsString(submission.events).contains(OTHER_SPEAKER))
        assertTrue(OTHER_SPEAKER in submission.speakerNames)
    }

    @Test
    fun `검증을 통과하면 상대 이름을 화자 표시로 바꿔 담아 대기열에 넣고 원본을 지운다`() {
        val job = upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT))

        analysisUploadWorkerFacade.validateNextUpload()

        assertEquals("QUEUED", analysisStatusFacade.getStatus(job).status)
        val payload = analysisJobRepository.findById(job.jobId.toString()).orElseThrow().requirePayload()
        assertTrue(payload.contains("고마워 0"))
        assertTrue(payload.contains(OTHER_TEXT))
        assertSpeakerOnlyInNames(payload)
        assertFalse(analysisUploadRepository.existsById(job.jobId.toString()))
    }

    @Test
    fun `본인 메시지가 제출 상한보다 많아도 조서 집계는 고르기 전 분석 기간 전체로 세어 페이로드에 담고 요청 해시는 그대로다`() {
        val job = upload(TestChatFiles.kakaoIos(OPEN_TIME, AnalysisEvents.MAX_EVENTS + EXTRA_OWN_COUNT))
        val requestHash = storedJob(job).requestHash
        clock.moveTo(OPEN_TIME.plus(ALL_SENT))

        analysisUploadWorkerFacade.validateNextUpload()

        val stored = storedJob(job)
        val submission = objectMapper.readValue(stored.requirePayload(), AnalysisSubmission::class.java)
        assertEquals(AnalysisEvents.MAX_EVENTS, submission.events.toList().count { event -> event.isSelf() })
        val dossier = assertNotNull(submission.dossier)
        assertEquals(AnalysisEvents.MAX_EVENTS + EXTRA_OWN_COUNT, dossier.stats.messageCount())
        assertEquals(DossierStats.VERSION, dossier.stats.version)
        assertEquals(requestHash, stored.requestHash)
    }

    @Test
    fun `검증을 마친 원본을 같은 멱등 키와 같은 파일로 다시 보내면 페이로드에 조서 집계가 있어도 처음 작업을 돌려준다`() {
        val content = TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT)
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val key = "key-" + UUID.randomUUID()
        val first = analysisFacade.upload(UploadAnalysisCommand(participantId, key, DataSource.KAKAO.name, SELF_SPEAKER, null) { content })
        analysisUploadWorkerFacade.validateNextUpload()

        val replay = analysisFacade.upload(UploadAnalysisCommand(participantId, key, DataSource.KAKAO.name, SELF_SPEAKER, null) { content })

        assertEquals(first.id, replay.id)
        assertTrue(analysisJobRepository.findById(first.id).orElseThrow().requirePayload().contains("\"dossier\""))
    }

    @Test
    fun `인스타그램 JSON도 검증을 통과하면 상대 이름을 화자 표시로 바꿔 담아 대기열에 넣고 원본을 지운다`() {
        val job = upload(TestChatFiles.instagram(OPEN_TIME, EVENT_COUNT), source = DataSource.INSTAGRAM)

        analysisUploadWorkerFacade.validateNextUpload()

        assertEquals("QUEUED", analysisStatusFacade.getStatus(job).status)
        val payload = analysisJobRepository.findById(job.jobId.toString()).orElseThrow().requirePayload()
        assertTrue(payload.contains("고마워 0"))
        assertTrue(payload.contains("INSTAGRAM"))
        assertTrue(payload.contains(OTHER_TEXT))
        assertSpeakerOnlyInNames(payload)
        assertFalse(analysisUploadRepository.existsById(job.jobId.toString()))
    }

    @Test
    fun `검증 중 예상하지 못한 예외가 나면 ANALYSIS_FAILED로 끝내고 원본을 지운다`() {
        willThrow(IllegalStateException("예상하지 못한 오류")).given(analysisUploadService).normalize(anyUpload(), anyInstant(), anyTally())

        assertRejected(upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT)), "FAILED", "ANALYSIS_FAILED")
    }

    @Test
    fun `대기열 저장이 일시적으로 실패하면 작업과 원본을 남겨 다음 폴링에서 다시 시도한다`() {
        willThrow(DataAccessResourceFailureException("잠깐 끊김")).willCallRealMethod().given(analysisUploadService).delete(anyJobId())
        val job = upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT))

        analysisUploadWorkerFacade.validateNextUpload()

        assertEquals("VALIDATING", analysisStatusFacade.getStatus(job).status)
        assertTrue(analysisUploadRepository.existsById(job.jobId.toString()))

        analysisUploadWorkerFacade.validateNextUpload()

        assertEquals("QUEUED", analysisStatusFacade.getStatus(job).status)
        assertFalse(analysisUploadRepository.existsById(job.jobId.toString()))
    }

    @Test
    fun `대화 형식이 아니면 UNSUPPORTED_FILE_FORMAT으로 끝내고 원본을 지운다`() {
        assertRejected(upload("메모일 뿐입니다".toByteArray()), "FAILED", "UNSUPPORTED_FILE_FORMAT")
    }

    @Test
    fun `인스타그램 파일이 올바른 JSON이 아니면 UNSUPPORTED_FILE_FORMAT으로 끝내고 원본을 지운다`() {
        assertRejected(upload("{\"messages\": [".toByteArray(), source = DataSource.INSTAGRAM), "FAILED", "UNSUPPORTED_FILE_FORMAT")
    }

    @Test
    fun `인스타그램 JSON이 null이거나 대화방 모양이 아니면 UNSUPPORTED_FILE_FORMAT으로 끝낸다`() {
        assertRejected(upload("null".toByteArray(), source = DataSource.INSTAGRAM), "FAILED", "UNSUPPORTED_FILE_FORMAT")
        assertRejected(upload("[]".toByteArray(), source = DataSource.INSTAGRAM), "FAILED", "UNSUPPORTED_FILE_FORMAT")
        assertRejected(upload("{\"messages\":{}}".toByteArray(), source = DataSource.INSTAGRAM), "FAILED", "UNSUPPORTED_FILE_FORMAT")
        assertRejected(upload("{\"messages\":[null]}".toByteArray(), source = DataSource.INSTAGRAM), "FAILED", "UNSUPPORTED_FILE_FORMAT")
    }

    @Test
    fun `본인 이름의 메시지가 없으면 SPEAKER_NOT_IDENTIFIED로 끝낸다`() {
        assertRejected(upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT), "없는 사람"), "FAILED", "SPEAKER_NOT_IDENTIFIED")
    }

    @Test
    fun `본인 이벤트가 20개보다 적으면 INSUFFICIENT_EVENTS로 끝낸다`() {
        assertRejected(upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT - 1)), "FAILED", "INSUFFICIENT_EVENTS")
    }

    @Test
    fun `24시간 넘게 검증을 기다린 작업은 EXPIRED로 끝내고 원본을 지운다`() {
        val job = upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT))
        clock.moveTo(OPEN_TIME.plus(AnalysisJob.QUEUE_EXPIRY).plusSeconds(1))

        analysisUploadWorkerFacade.validateNextUpload()

        assertRejected(job, "FAILED", "EXPIRED")
    }

    @Test
    fun `검증 시도를 다 쓴 작업은 RETRY_EXHAUSTED로 끝내고 원본을 지운다`() {
        val job = upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT))
        val stored = analysisJobRepository.findById(job.jobId.toString()).orElseThrow()
        repeat(AnalysisJob.MAX_ATTEMPTS) { stored.startValidation(OPEN_TIME) }
        analysisJobRepository.save(stored)

        analysisUploadWorkerFacade.validateNextUpload()

        assertRejected(job, "FAILED", "RETRY_EXHAUSTED")
    }

    private fun upload(
        content: ByteArray,
        selfSpeaker: String = SELF_SPEAKER,
        source: DataSource = DataSource.KAKAO,
    ): AnalysisJobCommand {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val command = UploadAnalysisCommand(participantId, "key-" + UUID.randomUUID(), source.name, selfSpeaker, null) { content }
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.upload(command).id))
    }

    /** Kotlin의 null 불가 매개변수에도 쓸 수 있도록 매처를 등록한 뒤 null 대신 빈 원본을 돌려준다. */
    private fun anyUpload(): AnalysisUpload =
        any(AnalysisUpload::class.java) ?: AnalysisUpload("", DataSource.KAKAO, SELF_SPEAKER, ByteArray(0), OPEN_TIME)

    private fun storedJob(job: AnalysisJobCommand): AnalysisJob = analysisJobRepository.findById(job.jobId.toString()).orElseThrow()

    private fun anyJobId(): String = any(String::class.java) ?: ""

    private fun anyInstant(): Instant = any(Instant::class.java) ?: OPEN_TIME

    /** 매처를 등록한 뒤 null 대신 아무것도 세지 않는 조서 집계 함수를 돌려준다. */
    @Suppress("UNCHECKED_CAST")
    private fun anyTally(): (DataSource, AnalysisEvents) -> DossierTally? =
        any(Function2::class.java) as ((DataSource, AnalysisEvents) -> DossierTally?)? ?: { _, _ -> null }

    /** 검증 작업자를 한 번 돌린 뒤(이미 돌렸으면 남은 원본이 없어 아무 일도 없다) 상태와 원본 삭제를 확인한다. */
    private fun assertRejected(job: AnalysisJobCommand, status: String, errorCode: String) {
        analysisUploadWorkerFacade.validateNextUpload()
        val result = analysisStatusFacade.getStatus(job)
        assertEquals(status, result.status)
        assertEquals(errorCode, result.errorCode)
        assertFalse(analysisUploadRepository.existsById(job.jobId.toString()))
    }

    companion object {
        private const val EVENT_COUNT = 20

        /** 본인 이벤트 상한을 넘겨 고르기에서 빠지는 메시지 수. */
        private const val EXTRA_OWN_COUNT = 100

        /** 테스트 파일은 하루 전부터 1분 간격이라, 상한을 넘는 메시지가 모두 과거가 되도록 검증 시각을 이만큼 늦춘다. */
        private val ALL_SENT: Duration = Duration.ofHours(12)
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
