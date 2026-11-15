package com.santacut.application.participant.facade

import com.santacut.application.global.Facade
import com.santacut.application.participant.command.CurrentParticipantCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.result.CurrentParticipantResult
import com.santacut.application.participant.result.RegisteredParticipantResult
import com.santacut.domain.participant.model.Nickname
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.participant.service.RegionService
import com.santacut.domain.session.service.SessionService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Facade
class ParticipantFacade(
    private val participantService: ParticipantService,
    private val regionService: RegionService,
    private val sessionService: SessionService,
    private val clock: Clock,
) {
    /** 닉네임과 지역을 확인해 참가자를 등록하고 바로 로그인 세션을 연다. */
    @Transactional
    fun register(command: RegisterParticipantCommand): RegisteredParticipantResult {
        val nickname = Nickname.from(command.nickname)
        regionService.verifySupported(command.regionCode)
        val now = clock.instant()
        val participant = participantService.register(nickname, command.regionCode, now)
        val sessionToken = sessionService.open(participant, now)
        return RegisteredParticipantResult.from(participant, sessionToken)
    }

    @Transactional(readOnly = true)
    fun getCurrentParticipant(command: CurrentParticipantCommand): CurrentParticipantResult =
        CurrentParticipantResult.from(participantService.get(command.participantId))
}
