package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.analysis.model.ScoringMessages
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * AI 사용자 메시지의 짧은 글 형식(항목별 신호 줄, 주 머리줄, `번호|본문` 줄)을 확인한다.
 */
class ScoreModelInputTest {
    @Test
    fun `신호 블록 다음에 시각순 표본을 주가 바뀔 때만 머리줄을 넣어 번호와 본문으로 쓴다`() {
        val messages = ScoringMessages(
            listOf(
                ScoringMessage(LocalDateTime.of(2026, 11, 14, 23, 10), "내일 같이 만나\n고마워"),
                ScoringMessage(LocalDateTime.of(2026, 11, 15, 8, 5), "ㅋㅋㅋ"),
                ScoringMessage(LocalDateTime.of(2026, 11, 15, 13, 0), "점심 뭐 먹었어"),
                ScoringMessage(LocalDateTime.of(2026, 11, 16, 9, 0), "미안 늦었어"),
            ),
        )
        val sample = messages.sample()
        val digest = ScoringDigest(DataSource.INSTAGRAM, messages.toFeatures(), messages.signals(sample), sample)

        assertEquals(
            """
            [signals]
            care_empathy hit=0 weeks=0/2 shown=0
            positivity_support hit=1 weeks=1/2 shown=1
            respectful_communication hit=1 weeks=1/2 shown=1
            cooperation_sharing hit=1 weeks=1/2 shown=1
            [sample]
            # 11-09주
            1|내일 같이 만나 고마워
            2|점심 뭐 먹었어
            # 11-16주
            3|미안 늦었어
            """.trimIndent(),
            ScoreModelInput.from(digest).toText(),
        )
    }
}
