package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 메시지 전체로 대화문 줄을 만드는 규칙(자리표시 빼기, 같은 화자 잇기)을 확인한다.
 */
class ScoringMessagesTest {
    private val start = LocalDateTime.of(2026, 11, 1, 9, 0)

    @Test
    fun `같은 화자가 3시간 안에 잇달아 보낸 메시지는 한 줄로 잇는다`() {
        val turns = ScoringMessages(
            listOf(
                at(0, "상대", "오늘 발표 망했어"),
                at(1, "나", "헐 괜찮아?"),
                at(2, "나", "끝나고 밥 먹자"),
                at(MERGE_GAP_MINUTES + 2, "나", "잘 들어갔어?"),
            ),
        ).toTurns()

        assertEquals(listOf("상대|오늘 발표 망했어", "나|헐 괜찮아? / 끝나고 밥 먹자", "나|잘 들어갔어?"), linesOf(turns))
    }

    @Test
    fun `자리표시와 정규화 뒤 빈 메시지는 빼고 그 앞뒤 같은 화자 메시지를 잇는다`() {
        val messages = ScoringMessages(listOf(at(0, "나", "이거 봐"), at(1, "상대", "사진"), at(2, "나", "  "), at(3, "나", "귀엽지")))

        assertEquals(listOf("나|이거 봐 / 귀엽지"), linesOf(messages.toTurns()))
        assertEquals(2, messages.selfCount())
    }

    private fun linesOf(turns: TranscriptTurns): List<String> = turns.mapNumbered { _, turn, _ -> "${turn.speaker}|${turn.text}" }

    private fun at(minutes: Long, speaker: String, text: String): ScoringMessage = ScoringMessage(start.plusMinutes(minutes), text, speaker)

    companion object {
        private const val MERGE_GAP_MINUTES = 180L
    }
}
