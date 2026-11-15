package com.santacut.domain.session.model

import com.santacut.domain.global.model.Sha256
import com.santacut.domain.global.model.UtcInstantConverter
import com.santacut.domain.participant.model.Participant
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 로그인 세션. 토큰 원문은 저장하지 않고 SHA-256 해시만 저장한다.
 */
@Entity
@Table(name = "sessions")
class ParticipantSession(
    tokenHash: String,
    userId: String,
    expiresAt: Instant,
) {
    @Id
    @Column(name = "token_hash")
    var tokenHash: String = tokenHash
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String = userId
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = expiresAt
        protected set

    fun isActiveAt(now: Instant): Boolean = expiresAt.isAfter(now)

    fun participantId(): UUID = UUID.fromString(userId)

    companion object {
        /** 세션 유효 기간. 웹 계층은 세션 쿠키의 유효 기간도 이 값으로 맞춘다. */
        val LIFETIME: Duration = Duration.ofDays(30)

        fun open(token: String, participant: Participant, openedAt: Instant): ParticipantSession =
            ParticipantSession(hashOf(token), participant.id, openedAt.plus(LIFETIME))

        fun hashOf(token: String): String = Sha256.hex(token)
    }
}
