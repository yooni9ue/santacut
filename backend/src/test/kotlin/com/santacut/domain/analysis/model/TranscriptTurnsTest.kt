package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 대화문 줄 목록의 글자 수 통계와 최근 줄만 남기는 길이 한도를 확인한다.
 */
class TranscriptTurnsTest {
    private val noon = LocalDateTime.of(2026, 11, 14, 12, 0)
    private val turns = TranscriptTurns(listOf(turn("나", "가나다"), turn("상대", "라마"), turn("나", "바사아자")))

    @Test
    fun `본인 본문 글자 수와 전체 글자 수를 센다`() {
        assertEquals(7, turns.selfLength())
        assertEquals(9, turns.totalLength())
    }

    @Test
    fun `길이 한도를 넘으면 최근 줄부터 한도 안까지만 시각순으로 남긴다`() {
        val kept = turns.recentWithin(6)

        assertEquals(listOf("라마", "바사아자"), kept.mapNumbered { _, turn, _ -> turn.text })
        assertEquals(3, turns.recentWithin(9).size())
    }

    @Test
    fun `번호는 1부터 붙이고 앞 줄을 함께 넘긴다`() {
        assertEquals(listOf("1:null", "2:가나다", "3:라마"), turns.mapNumbered { number, _, previous -> "$number:${previous?.text}" })
    }

    private fun turn(speaker: String, text: String): TranscriptTurn = TranscriptTurn(speaker, noon, noon, text)
}
