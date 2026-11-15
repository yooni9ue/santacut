package com.santacut.application.facade

import com.santacut.application.command.CurrentParticipantCommand
import com.santacut.application.command.RegisterParticipantCommand
import com.santacut.application.result.CurrentParticipantResult
import com.santacut.application.result.RegisteredParticipantResult
import com.santacut.domain.model.Nickname
import com.santacut.domain.service.ParticipantService
import com.santacut.domain.service.RegionService
import com.santacut.domain.service.SessionService
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
