package com.santacut.domain.dossier.model

import com.santacut.TestDossiers
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 고맙다는 말 횟수 지표. 본인 말풍선 가운데 고마워·고맙·ㄱㅅ·감사·땡큐가 든 말풍선 수다. */
class ThanksCountTest {
    @Test
    fun `고맙다는 말이 든 본인 말풍선만 하나씩 센다`() {
        val messages = TestDossiers.selves(listOf("고마워 고마워", "정말 고맙다", "ㄱㅅㄱㅅ", "감사합니다", "땡큐", "내일 보자"))

        assertEquals(5, TestDossiers.tally(messages).stats.words.thanksCount)
    }

    @Test
    fun `상대가 한 고맙다는 말은 세지 않는다`() {
        val messages = listOf(
            TestDossiers.partner(AT, "고마워"),
            TestDossiers.self(AT.plusMinutes(1), "응 내일 보자"),
        )

        assertEquals(0, TestDossiers.tally(messages).stats.words.thanksCount)
    }

    @Test
    fun `이 값이 생기기 전 집계 JSON은 횟수 없이 읽는다`() {
        val stats = TestDossiers.tally(TestDossiers.selves(listOf("안녕"))).stats
        val json = TestDossiers.mapper.writeValueAsString(stats).replace(",\"thanks_count\":0", "")

        assertNull(TestDossiers.mapper.readValue(json, DossierStats::class.java).words.thanksCount)
    }

    companion object {
        private val AT: LocalDateTime = LocalDateTime.of(2026, 3, 2, 10, 0)
    }
}
