package com.santacut.application.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.command.AnalysisEventCommand
import com.santacut.application.command.AnalysisEventCommands
import com.santacut.application.command.AnalysisJobCommand
import com.santacut.application.command.RegisterParticipantCommand
import com.santacut.application.command.SubmitAnalysisCommand
import com.santacut.domain.exception.ErrorCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@IntegrationTest
class AnalysisFacadeTest @Autowired constructor(
    private val analysisFacade: AnalysisFacade,
    private val participantFacade: ParticipantFacade,
    clock: Clock,
) {
    private val occurredAt = clock.instant().minus(Duration.ofDays(1))
    private val futureOccurredAt = clock.instant().plus(Duration.ofHours(1))

    @Test
    fun `분석을 제출하면 대기 상태 작업이 생기고 본인만 상태를 볼 수 있다`() {
        val participantId = registerParticipant()
        val submitted = analysisFacade.submit(submitCommand(participantId, newKey(), "고마워"))
        assertEquals("QUEUED", submitted.status)

        val status = analysisFacade.getStatus(AnalysisJobCommand(participantId, UUID.fromString(submitted.id)))
        assertEquals("QUEUED", status.status)
        assertNull(status.errorCode)
        assertNull(status.finishedAt)
        assertFalse(status.succeeded || status.failed)

        assertErrorCode(ErrorCode.NOT_FOUND) {
            analysisFacade.getStatus(AnalysisJobCommand(registerParticipant(), UUID.fromString(submitted.id)))
        }
    }

    @Test
    fun `같은 멱등 키로 같은 본문을 다시 보내면 처음 작업을, 다른 본문이면 충돌을 돌려준다`() {
        val participantId = registerParticipant()
        val key = newKey()
        val first = analysisFacade.submit(submitCommand(participantId, key, "고마워"))
        val replay = analysisFacade.submit(submitCommand(participantId, key, "고마워"))
        assertEquals(first.id, replay.id)
        assertErrorCode(ErrorCode.IDEMPOTENCY_CONFLICT) {
            analysisFacade.submit(submitCommand(participantId, key, "다른 본문"))
        }
    }

    @Test
    fun `24시간 동안 세 번까지만 제출할 수 있다`() {
        val participantId = registerParticipant()
        repeat(MAX_DAILY_SUBMISSIONS) { analysisFacade.submit(submitCommand(participantId, newKey(), "고마워")) }
        assertErrorCode(ErrorCode.DAILY_USER_LIMIT) {
            analysisFacade.submit(submitCommand(participantId, newKey(), "고마워"))
        }
    }

    @Test
    fun `잘못된 멱등 키, 범위 밖 이벤트, 지워진 참가자는 거절한다`() {
        val participantId = registerParticipant()
        assertErrorCode(ErrorCode.INVALID_IDEMPOTENCY_KEY) {
            analysisFacade.submit(submitCommand(participantId, "short", "고마워"))
        }
        val futureEvents = AnalysisEventCommands(List(EVENT_COUNT) { AnalysisEventCommand(futureOccurredAt, "미래") })
        assertErrorCode(ErrorCode.INVALID_EVENT_RANGE) {
            analysisFacade.submit(SubmitAnalysisCommand(participantId, newKey(), SOURCE, futureEvents))
        }
        assertErrorCode(ErrorCode.NOT_AUTHENTICATED) {
            analysisFacade.submit(submitCommand(UUID.randomUUID(), newKey(), "고마워"))
        }
    }

    private fun registerParticipant(): UUID =
        participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id

    private fun submitCommand(participantId: UUID, key: String, text: String): SubmitAnalysisCommand {
        val events = List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt, "$text $index") }
        return SubmitAnalysisCommand(participantId, key, SOURCE, AnalysisEventCommands(events))
    }

    private fun newKey(): String = "key-" + UUID.randomUUID()

    companion object {
        private const val SOURCE = "KAKAO"
        private const val EVENT_COUNT = 20
        private const val MAX_DAILY_SUBMISSIONS = 3
    }
}
