package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.analysis.model.ScoringMessages
import com.santacut.domain.analysis.model.TranscriptTurn
import com.santacut.domain.analysis.model.TranscriptTurns
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AI 사용자 메시지의 대화문 형식(`번호|화자|본문` 줄, 간격 줄, 뺀 줄 머리줄)을 확인한다.
 */
class ScoreModelInputTest {
    @Test
    fun `대화문은 턴마다 번호와 화자를 붙이고 3시간 이상 벌어지면 간격 줄을 넣는다`() {
        val messages = ScoringMessages(
            listOf(
                ScoringMessage(LocalDateTime.of(2026, 11, 14, 23, 10), "오늘 발표 망했어ㅠㅠㅠㅠ", "상대"),
                ScoringMessage(LocalDateTime.of(2026, 11, 14, 23, 11), "헐 괜찮아?"),
                ScoringMessage(LocalDateTime.of(2026, 11, 14, 23, 12), "끝나고 밥 먹자"),
                ScoringMessage(LocalDateTime.of(2026, 11, 15, 4, 0), "사진", "상대"),
                ScoringMessage(LocalDateTime.of(2026, 11, 15, 4, 30), "고마워", "상대"),
                ScoringMessage(LocalDateTime.of(2026, 11, 17, 9, 0), "또 보자"),
            ),
        )
        val expected = """
            [대화]
            1|상대|오늘 발표 망했어ㅠㅠ
            2|나|헐 괜찮아? / 끝나고 밥 먹자
            (5시간 뒤)
            3|상대|고마워
            (2일 뒤)
            4|나|또 보자
        """.trimIndent()

        assertEquals(expected, ScoreModelInput.from(ScoringDigest.of(messages)).toText())
    }

    @Test
    fun `길이 한도로 뺀 줄이 있으면 머리에 그 수를 적는다`() {
        val noon = LocalDateTime.of(2026, 11, 14, 12, 0)
        val digest = ScoringDigest(3, 1, TranscriptTurns(listOf(TranscriptTurn("나", noon, noon, "고마워"))), 2)

        assertTrue(ScoreModelInput.from(digest).toText().startsWith("[대화]\n(앞선 2줄은 길이 한도로 뺐다)\n1|나|고마워"))
    }
}
