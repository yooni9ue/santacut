package com.santacut.application.analysis.facade

import com.santacut.MutableClock
import com.santacut.TestChatFiles
import com.santacut.TestChatFiles.OTHER_TEXT
import com.santacut.TestChatFiles.SELF_SPEAKER
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.UploadAnalysisCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.DataSource
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.analysis.repository.AnalysisUploadRepository
import com.santacut.infrastructure.analysis.repository.DailyBudgetRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 원본 파일 검증 작업자가 성공·실패·만료·시도 소진을 처리하고 어느 쪽이든 원본을 지우는지 확인한다.
 * 분석 작업자 테스트와 같은 설정을 써서 컨텍스트와 `build/worker-test.db`를 함께 쓰고, 테스트마다 비운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-test.db",
        "app.season-year=${AnalysisWorkerFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.ai.daily-call-limit=${AnalysisWorkerFacadeTest.DAILY_CALL_LIMIT}",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class AnalysisUploadWorkerFacadeTest @Autowired constructor(
    private val analysisUploadWorkerFacade: AnalysisUploadWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val analysisUploadRepository: AnalysisUploadRepository,
    private val dailyBudgetRepository: DailyBudgetRepository,
) {
    @BeforeEach
    fun setUp() {
        analysisUploadRepository.deleteAll()
        analysisJobRepository.deleteAll()
        dailyBudgetRepository.deleteAll()
        clock.moveTo(OPEN_TIME)
        given(scoreClient.isEnabled()).willReturn(true)
    }

    @Test
    fun `검증을 통과하면 본인 메시지만 담아 대기열에 넣고 원본을 지운다`() {
        val job = upload(TestChatFiles.kakaoIos(OPEN_TIME, EVENT_COUNT))

        analysisUploadWorkerFacade.validateNextUpload()

        assertEquals("QUEUED", analysisFacade.getStatus(job).status)
        val payload = analysisJobRepository.findById(job.jobId.toString()).orElseThrow().requirePayload()
        assertTrue(payload.contains("고마워 0"))
        assertFalse(payload.contains(OTHER_TEXT))
        assertFalse(analysisUploadRepository.existsById(job.jobId.toString()))
    }

    @Test
    fun `대화 형식이 아니면 UNSUPPORTED_FILE_FORMAT으로 끝내고 원본을 지운다`() {
        assertRejected(upload("메모일 뿐입니다".toByteArray()), "FAILED", "UNSUPPORTED_FILE_FORMAT")
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

    private fun upload(content: ByteArray, selfSpeaker: String = SELF_SPEAKER): AnalysisJobCommand {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val command = UploadAnalysisCommand(participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, selfSpeaker, content)
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.upload(command).id))
    }

    /** 검증 작업자를 한 번 돌린 뒤(이미 돌렸으면 남은 원본이 없어 아무 일도 없다) 상태와 원본 삭제를 확인한다. */
    private fun assertRejected(job: AnalysisJobCommand, status: String, errorCode: String) {
        analysisUploadWorkerFacade.validateNextUpload()
        val result = analysisFacade.getStatus(job)
        assertEquals(status, result.status)
        assertEquals(errorCode, result.errorCode)
        assertFalse(analysisUploadRepository.existsById(job.jobId.toString()))
    }

    companion object {
        private const val EVENT_COUNT = 20
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
