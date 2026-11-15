package com.santacut.domain.service

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import com.santacut.domain.model.Nickname
import com.santacut.domain.model.Participant
import com.santacut.infrastructure.repository.ParticipantRepository
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

    fun get(participantId: UUID): Participant =
        participantRepository.findById(participantId.toString()).orElse(null)
            ?: throw SantaCutException(ErrorCode.PARTICIPANT_NOT_FOUND)

    /** 세션은 있지만 참가자가 지워졌다면 인증되지 않은 것으로 본다. */
    fun verifyRegistered(participantId: UUID) {
        if (!participantRepository.existsById(participantId.toString())) {
            throw SantaCutException(ErrorCode.NOT_AUTHENTICATED)
        }
    }
}
