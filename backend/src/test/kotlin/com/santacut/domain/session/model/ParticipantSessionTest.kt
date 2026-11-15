package com.santacut.domain.session.model

import com.santacut.domain.global.model.Sha256
import com.santacut.domain.participant.model.Participant
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipantSessionTest {
    private val openedAt = Instant.parse("2026-11-15T00:00:00Z")
    private val participant = Participant(UUID.randomUUID().toString(), "산타", "산타", "KR-11-110", openedAt)

    @Test
    fun `세션을 열면 토큰 원문 대신 SHA-256 해시를 저장하고 30일 뒤 만료한다`() {
        val session = ParticipantSession.open(TOKEN, participant, openedAt)
        assertEquals(Sha256.hex(TOKEN), session.tokenHash)
        assertEquals(ParticipantSession.hashOf(TOKEN), session.tokenHash)
        assertEquals(openedAt.plus(Duration.ofDays(30)), session.expiresAt)
        assertEquals(participant.participantId(), session.participantId())
    }

    @Test
    fun `만료 시각 전까지만 유효하고 만료 시각부터는 유효하지 않다`() {
        val session = ParticipantSession.open(TOKEN, participant, openedAt)
        assertTrue(session.isActiveAt(session.expiresAt.minusMillis(1)))
        assertFalse(session.isActiveAt(session.expiresAt))
        assertFalse(session.isActiveAt(session.expiresAt.plusSeconds(1)))
    }

    companion object {
        private const val TOKEN = "session-token-0123456789abcdef0123456789"
    }
}
