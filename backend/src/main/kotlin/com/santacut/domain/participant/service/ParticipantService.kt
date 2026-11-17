package com.santacut.domain.participant.service

import com.santacut.domain.global.config.ClientRateLimitProperties
import com.santacut.domain.global.config.RegionChangeProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.ClientRateLimit
import com.santacut.domain.participant.model.Nickname
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.participant.model.RegionChange
import com.santacut.domain.participant.model.RegionChangeLimit
import com.santacut.infrastructure.participant.repository.ParticipantRepository
import com.santacut.infrastructure.participant.repository.RegionChangeRepository
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ParticipantService(
    private val participantRepository: ParticipantRepository,
    private val regionChangeRepository: RegionChangeRepository,
    clientRateLimitProperties: ClientRateLimitProperties,
    regionChangeProperties: RegionChangeProperties,
) {
    private val regionChangeLimit = RegionChangeLimit(
        regionChangeProperties.maxPerWindow, regionChangeProperties.window, regionChangeProperties.maxPerSeason,
    )

    private val registrationLimit = ClientRateLimit(
        clientRateLimitProperties.registrationsPerClient, clientRateLimitProperties.registrationWindow,
    )

    /**
     * 닉네임 키의 유일 제약으로 동시 등록 경쟁에서도 중복을 막는다.
     * 데이터 접근 예외 전체를 받아 닉네임 충돌인지는 [Participant.isNicknameConflict]에 맡긴다.
     * 같은 IP([clientIp])의 등록 시도가 한도를 넘으면 CLIENT_RATE_LIMITED다. 참가자를 계속 새로 만들어 참가자별 한도를 피하지 못하게 한다.
     */
    fun register(nickname: Nickname, regionCode: String, registeredAt: Instant, clientIp: String?): Participant {
        registrationLimit.acquire(clientIp, registeredAt)
        try {
            return participantRepository.saveAndFlush(Participant.register(nickname, regionCode, registeredAt))
        } catch (exception: DataAccessException) {
            val nicknameTaken = Participant.isNicknameConflict(exception.mostSpecificCause.message)
            throw if (nicknameTaken) SantaCutException(ErrorCode.NICKNAME_TAKEN) else exception
        }
    }

    /** 대소문자를 구분하지 않는 닉네임 키로 아직 아무도 쓰지 않는지 본다. */
    fun isNicknameAvailable(nickname: Nickname): Boolean =
        !participantRepository.existsByNicknameKey(nickname.key())

    fun get(participantId: UUID): Participant =
        participantRepository.findById(participantId.toString()).orElse(null)
            ?: throw SantaCutException(ErrorCode.PARTICIPANT_NOT_FOUND)

    /**
     * 지역 변경 한도를 확인한 뒤 [participant]의 지역을 바꾸고 변경을 기록한다.
     * 한도를 넘으면 REGION_CHANGE_LIMITED(retry_at 있음)나 REGION_CHANGE_SEASON_LIMIT이다.
     */
    fun changeRegion(participant: Participant, regionCode: String, now: Instant) {
        verifyRegionChangeAllowed(participant.id, now)
        participant.changeRegion(regionCode)
        regionChangeRepository.save(RegionChange.record(participant, now))
    }

    /**
     * 참가자를 지운다. 세션, 분석 작업, 평가, 순위 기록, 공유 링크는 DB의 ON DELETE CASCADE로 함께 지워진다.
     * 이어지는 순위 재계산이 지워진 행을 보도록 바로 반영한다.
     */
    fun delete(participantId: UUID) {
        participantRepository.delete(get(participantId))
        participantRepository.flush()
    }

    /** 세션은 있지만 참가자가 지워졌다면 인증되지 않은 것으로 본다. */
    fun verifyRegistered(participantId: UUID) {
        if (!participantRepository.existsById(participantId.toString())) {
            throw SantaCutException(ErrorCode.NOT_AUTHENTICATED)
        }
    }

    private fun verifyRegionChangeAllowed(userId: String, now: Instant) {
        val windowStart = regionChangeLimit.windowStart(now)
        regionChangeLimit.verify(
            regionChangeRepository.countByUserId(userId),
            regionChangeRepository.countByUserIdAndChangedAtAfter(userId, windowStart),
            regionChangeRepository.findFirstByUserIdAndChangedAtAfterOrderByChangedAtAsc(userId, windowStart)?.changedAt,
        )
    }
}
