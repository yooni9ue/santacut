package com.santacut.application.analysis.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.MutableClock
import com.santacut.TestChatFiles
import com.santacut.TestChatFiles.SELF_SPEAKER
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisEventCommand
import com.santacut.application.analysis.command.AnalysisEventCommands
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.command.UploadAnalysisCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.analysis.repository.AnalysisUploadRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/**
 * 원본 파일 업로드가 시즌 마감(409), 하루 제출 한도(429, JSON 제출과 함께 셈), AI 미설정(503)에서 거절되는지 확인한다.
 * 시각을 옮기고 AI 설정을 바꿔야 하므로 분석 작업자 테스트와 같은 설정(같은 컨텍스트, `build/worker-test.db`)을 쓴다.
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
class AnalysisUploadFacadeTest @Autowired constructor(
    private val analysisFacade: AnalysisFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val analysisUploadRepository: AnalysisUploadRepository,
) {
    private val seasonClose: Instant = Season(AnalysisWorkerFacadeTest.SEASON_YEAR).deliveryStart().toInstant()

    @BeforeEach
    fun setUp() {
        analysisUploadRepository.deleteAll()
        analysisJobRepository.deleteAll()
        clock.moveTo(OPEN_TIME)
        given(scoreClient.isEnabled()).willReturn(true)
    }

    @Test
    fun `시즌이 마감되면 원본 파일을 받지 않고 SEASON_FROZEN이다`() {
        val participantId = registerParticipant()
        clock.moveTo(seasonClose)

        assertErrorCode(ErrorCode.SEASON_FROZEN) { analysisFacade.upload(uploadCommand(participantId)) }
        assertEquals(0, analysisUploadRepository.count())
    }

    @Test
    fun `JSON 제출과 원본 파일 제출을 합쳐 24시간에 세 번을 넘기면 DAILY_USER_LIMIT이다`() {
        val participantId = registerParticipant()
        analysisFacade.submit(submitCommand(participantId))
        analysisFacade.upload(uploadCommand(participantId))
        analysisFacade.submit(submitCommand(participantId))

        assertErrorCode(ErrorCode.DAILY_USER_LIMIT) { analysisFacade.upload(uploadCommand(participantId)) }
        assertErrorCode(ErrorCode.DAILY_USER_LIMIT) { analysisFacade.submit(submitCommand(participantId)) }
        assertEquals(1, analysisUploadRepository.count())
    }

    @Test
    fun `AI 모델이 설정되지 않았으면 원본 파일을 받지 않고 AI_NOT_CONFIGURED다`() {
        given(scoreClient.isEnabled()).willReturn(false)
        val participantId = registerParticipant()

        assertErrorCode(ErrorCode.AI_NOT_CONFIGURED) { analysisFacade.upload(uploadCommand(participantId)) }
        assertEquals(0, analysisJobRepository.count())
    }

    private fun registerParticipant(): UUID =
        participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id

    private fun uploadCommand(participantId: UUID): UploadAnalysisCommand = UploadAnalysisCommand(
        participantId,
        newKey(),
        DataSource.KAKAO.name,
        SELF_SPEAKER,
        TestChatFiles.kakaoIos(clock.instant(), EVENT_COUNT),
    )

    private fun submitCommand(participantId: UUID): SubmitAnalysisCommand {
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt, "고마워 $index") })
        return SubmitAnalysisCommand(participantId, newKey(), DataSource.KAKAO.name, events)
    }

    private fun newKey(): String = "key-" + UUID.randomUUID()

    companion object {
        private const val EVENT_COUNT = 20
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
