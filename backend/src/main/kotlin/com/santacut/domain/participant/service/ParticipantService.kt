package com.santacut.domain.participant.service

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.participant.model.Nickname
import com.santacut.domain.participant.model.Participant
import com.santacut.infrastructure.participant.repository.ParticipantRepository
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ParticipantService(
    private val participantRepository: ParticipantRepository,
) {
    /**
     * 닉네임 키의 유일 제약으로 동시 등록 경쟁에서도 중복을 막는다.
     * 데이터 접근 예외 전체를 받아 닉네임 충돌인지는 [Participant.isNicknameConflict]에 맡긴다.
     */
    fun register(nickname: Nickname, regionCode: String, registeredAt: Instant): Participant {
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

    fun changeRegion(participantId: UUID, regionCode: String): Participant =
        get(participantId).also { it.changeRegion(regionCode) }

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
}
