package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ScoringDigest

/**
 * AI 모델에 보내는 사용자 메시지 본문. 정규화한 대화문을 한 줄에 한 턴씩 보낸다.
 * 길이 한도로 앞선 줄을 뺐으면 머리에 그 수를 적는다.
 *
 * ```text
 * [대화]
 * (앞선 120줄은 길이 한도로 뺐다)
 * 1|상대|오늘 발표 망했어 ㅠㅠ
 * 2|나|헐 괜찮아? / 끝나고 밥 먹자
 * (2일 뒤)
 * 3|상대1|고마워
 * ```
 */
class ScoreModelInput(
    private val omittedTurns: Int,
    private val lines: ScoreModelLines,
) {
    fun toText(): String = HEADER + omittedLine() + lines.toText()

    private fun omittedLine(): String =
        if (omittedTurns > 0) "$OMITTED_PREFIX$omittedTurns$OMITTED_SUFFIX$LINE_SEPARATOR" else ""

    companion object {
        private const val HEADER = "[대화]\n"
        private const val OMITTED_PREFIX = "(앞선 "
        private const val OMITTED_SUFFIX = "줄은 길이 한도로 뺐다)"
        private const val LINE_SEPARATOR = "\n"

        fun from(digest: ScoringDigest): ScoreModelInput =
            ScoreModelInput(digest.omittedTurns, ScoreModelLines.from(digest.turns))
    }
}
