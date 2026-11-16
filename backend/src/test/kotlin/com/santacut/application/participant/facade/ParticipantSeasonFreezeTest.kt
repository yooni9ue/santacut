package com.santacut.application.participant.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.MutableClock
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.facade.AnalysisWorkerFacadeTest
import com.santacut.application.analysis.facade.WorkerTestClockConfiguration
import com.santacut.application.participant.command.ChangeRegionCommand
import com.santacut.application.participant.command.CurrentParticipantCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.client.ScoreClient
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.test.assertEquals

/**
 * 시즌 마감 뒤에는 지역을 바꿀 수 없는지 확인한다.
 * 시각을 옮겨야 하므로 분석 작업자 테스트와 같은 설정(같은 컨텍스트, `build/worker-test.db`)을 쓴다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-test.db",
        "app.season-year=${AnalysisWorkerFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class ParticipantSeasonFreezeTest @Autowired constructor(
    private val participantFacade: ParticipantFacade,
    private val clock: MutableClock,
) {
    private val seasonClose = Season(AnalysisWorkerFacadeTest.SEASON_YEAR).deliveryStart().toInstant()

    @Test
    fun `시즌이 마감되면 지역을 바꿀 수 없고 지역은 그대로다`() {
        clock.moveTo(seasonClose.minusSeconds(1))
        val registered = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        clock.moveTo(seasonClose)
        assertErrorCode(ErrorCode.SEASON_FROZEN) {
            participantFacade.changeRegion(ChangeRegionCommand(registered.id, OTHER_REGION_CODE))
        }
        assertEquals(REGION_CODE, participantFacade.getCurrentParticipant(CurrentParticipantCommand(registered.id)).regionCode)
    }

    companion object {
        private const val OTHER_REGION_CODE = "KR-11-650"
    }
}
