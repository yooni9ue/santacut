package com.santacut.application.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.command.CurrentParticipantCommand
import com.santacut.application.command.RegisterParticipantCommand
import com.santacut.application.command.SessionAuthenticationCommand
import com.santacut.domain.exception.ErrorCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@IntegrationTest
class ParticipantFacadeTest @Autowired constructor(
    private val participantFacade: ParticipantFacade,
    private val sessionFacade: SessionFacade,
) {
    @Test
    fun `참가자를 등록하면 그 세션 토큰으로 인증되고 내 정보를 조회할 수 있다`() {
        val nickname = uniqueNickname()
        val registered = participantFacade.register(RegisterParticipantCommand("  $nickname ", REGION_CODE))
        assertEquals(nickname, registered.nickname)
        assertEquals(REGION_CODE, registered.regionCode)

        val session = sessionFacade.authenticate(SessionAuthenticationCommand(registered.sessionToken))
        assertEquals(registered.id, session?.participantId)

        val current = participantFacade.getCurrentParticipant(CurrentParticipantCommand(registered.id))
        assertEquals(registered.id, current.id)
        assertEquals(nickname, current.nickname)
        assertEquals(REGION_CODE, current.regionCode)
    }

    @Test
    fun `대소문자만 다른 닉네임은 이미 쓰는 닉네임이다`() {
        val nickname = uniqueNickname()
        participantFacade.register(RegisterParticipantCommand(nickname, REGION_CODE))
        assertErrorCode(ErrorCode.NICKNAME_TAKEN) {
            participantFacade.register(RegisterParticipantCommand(nickname.uppercase(), REGION_CODE))
        }
    }

    @Test
    fun `형식이 틀린 닉네임이나 없는 지역으로는 등록할 수 없다`() {
        assertErrorCode(ErrorCode.INVALID_NICKNAME) {
            participantFacade.register(RegisterParticipantCommand("!", REGION_CODE))
        }
        assertErrorCode(ErrorCode.UNSUPPORTED_REGION) {
            participantFacade.register(RegisterParticipantCommand(uniqueNickname(), "KR-00-000"))
        }
    }

    @Test
    fun `모르는 세션 토큰은 인증되지 않고 없는 참가자는 조회할 수 없다`() {
        assertNull(sessionFacade.authenticate(SessionAuthenticationCommand("x".repeat(43))))
        assertErrorCode(ErrorCode.PARTICIPANT_NOT_FOUND) {
            participantFacade.getCurrentParticipant(CurrentParticipantCommand(UUID.randomUUID()))
        }
    }
}
