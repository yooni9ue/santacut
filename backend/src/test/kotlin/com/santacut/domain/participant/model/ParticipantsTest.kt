package com.santacut.domain.participant.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertNull
import kotlin.test.assertSame

class ParticipantsTest {
    private val registeredAt = Instant.parse("2026-11-01T00:00:00Z")
    private val santa = Participant("id-santa", "산타", "산타", "KR-11-110", registeredAt)
    private val rudolph = Participant("id-rudolph", "루돌프", "루돌프", "KR-11-140", registeredAt)

    @Test
    fun `참가자 ID로 참가자를 찾는다`() {
        val participants = Participants(listOf(santa, rudolph))
        assertSame(santa, participants.find("id-santa"))
        assertSame(rudolph, participants.find("id-rudolph"))
    }

    @Test
    fun `목록에 없는 ID는 찾지 못한다`() {
        assertNull(Participants(listOf(santa)).find("id-rudolph"))
        assertNull(Participants(emptyList()).find("id-santa"))
    }
}
