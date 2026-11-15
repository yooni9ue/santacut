package com.santacut.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipantTest {
    private val registeredAt = Instant.parse("2026-11-01T00:00:00Z")

    @Test
    fun `등록하면 새 UUID와 정규화한 닉네임, 소문자 비교 키를 가진다`() {
        val participant = Participant.register(Nickname.from("SantaKim"), "KR-11-110", registeredAt)
        assertEquals(UUID.fromString(participant.id), participant.participantId())
        assertEquals("SantaKim", participant.nickname)
        assertEquals("santakim", participant.nicknameKey)
        assertEquals("KR-11-110", participant.regionCode)
        assertEquals(registeredAt, participant.createdAt)
    }

    @Test
    fun `저장 오류 메시지에 닉네임 키 유일 제약 이름이 있으면 닉네임 충돌이다`() {
        val message = "[SQLITE_CONSTRAINT_UNIQUE] UNIQUE constraint failed: participants.nickname_key"
        assertTrue(Participant.isNicknameConflict(message))
    }

    @Test
    fun `다른 제약 위반이나 메시지가 없는 오류는 닉네임 충돌이 아니다`() {
        assertFalse(Participant.isNicknameConflict("UNIQUE constraint failed: participants.id"))
        assertFalse(Participant.isNicknameConflict(null))
    }
}
