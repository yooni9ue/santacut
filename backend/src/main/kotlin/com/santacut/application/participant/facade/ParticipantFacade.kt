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
     * 시즌 마감 전에만 지역을 바꾼다. 도착 예정 시각이 지역 좌표에 달려 있으므로 같은 트랜잭션에서 순위를 다시 계산한다.
     */
    @Transactional
    fun changeRegion(command: ChangeRegionCommand): CurrentParticipantResult {
        val now = clock.instant()
        rankingService.verifySeasonOpenAt(now)
        regionService.verifySupported(command.regionCode)
        val participant = participantService.changeRegion(command.participantId, command.regionCode)
        rankingService.rebuild(now)
        return CurrentParticipantResult.from(participant)
    }

    /** 참가자와 그 데이터를 지우고 남은 참가자로 순위를 다시 계산한다. 시즌 마감 뒤에는 순위 계산이 스스로 건너뛴다. */
    @Transactional
    fun deleteMyData(command: DeleteParticipantCommand) {
        participantService.delete(command.participantId)
        rankingService.rebuild(clock.instant())
    }

    @Transactional(readOnly = true)
    fun getCurrentParticipant(command: CurrentParticipantCommand): CurrentParticipantResult =
        CurrentParticipantResult.from(participantService.get(command.participantId))
}
