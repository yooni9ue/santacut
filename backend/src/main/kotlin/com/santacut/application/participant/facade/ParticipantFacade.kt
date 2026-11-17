package com.santacut.application.participant.facade

import com.santacut.application.global.Facade
import com.santacut.application.participant.command.ChangeRegionCommand
import com.santacut.application.participant.command.CurrentParticipantCommand
import com.santacut.application.participant.command.DeleteParticipantCommand
import com.santacut.application.participant.command.NicknameAvailabilityCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.result.CurrentParticipantResult
import com.santacut.application.participant.result.NicknameAvailabilityResult
import com.santacut.application.participant.result.RegisteredParticipantResult
import com.santacut.domain.participant.model.Nickname
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.participant.service.RegionService
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.session.service.SessionService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Facade
class ParticipantFacade(
    private val participantService: ParticipantService,
    private val regionService: RegionService,
    private val sessionService: SessionService,
    private val rankingService: RankingService,
    private val clock: Clock,
) {
    /** 닉네임과 지역을 확인해 참가자를 등록하고 바로 로그인 세션을 연다. */
    @Transactional
    fun register(command: RegisterParticipantCommand): RegisteredParticipantResult {
        val nickname = Nickname.from(command.nickname)
        regionService.verifySupported(command.regionCode)
        val now = clock.instant()
        val participant = participantService.register(nickname, command.regionCode, now, command.clientIp)
        val sessionToken = sessionService.open(participant, now)
        return RegisteredParticipantResult.from(participant, sessionToken)
    }

    /** 형식이 틀리면 INVALID_NICKNAME이다. 확인만 하고 닉네임을 예약하지 않는다. */
    @Transactional(readOnly = true)
    fun checkNicknameAvailability(command: NicknameAvailabilityCommand): NicknameAvailabilityResult =
        NicknameAvailabilityResult(participantService.isNicknameAvailable(Nickname.from(command.nickname)))

    /**
     * 시즌 마감 전에만 지역을 바꾼다. 같은 지역이면 아무것도 바꾸지 않고 변경 횟수도 세지 않는다.
     * 변경 횟수 한도는 [ParticipantService.changeRegion]이 확인한다. 도착 예정 시각이 지역 좌표에 달려 있으므로 순위 다시 계산을 요청한다.
     */
    @Transactional
    fun changeRegion(command: ChangeRegionCommand): CurrentParticipantResult {
        val now = clock.instant()
        rankingService.verifySeasonOpenAt(now)
        regionService.verifySupported(command.regionCode)
        val participant = participantService.get(command.participantId)
        if (participant.isIn(command.regionCode)) return CurrentParticipantResult.from(participant)
        participantService.changeRegion(participant, command.regionCode, now)
        rankingService.requestRebuild()
        return CurrentParticipantResult.from(participant)
    }

    /** 참가자와 그 데이터를 지운다. 시즌 마감 전이면 순위 다시 계산을 요청하고, 마감 뒤에는 확정 순위를 그대로 둔다. */
    @Transactional
    fun deleteMyData(command: DeleteParticipantCommand) {
        participantService.delete(command.participantId)
        rankingService.forgetDeletedParticipant(clock.instant())
    }

    @Transactional(readOnly = true)
    fun getCurrentParticipant(command: CurrentParticipantCommand): CurrentParticipantResult =
        CurrentParticipantResult.from(participantService.get(command.participantId))
}
