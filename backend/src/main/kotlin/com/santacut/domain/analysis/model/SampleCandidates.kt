package com.santacut.domain.analysis.model

/**
 * 표본으로 뽑을 수 있는 본인 메시지. 무작위 없이 같은 입력에서는 늘 같은 표본을 고른다.
 *
 * 달력 주(월요일 시작)로 나누고, 주마다 항목 낱말이 든 메시지를 먼저, 그다음 이른 시각 순서로 줄 세운다.
 * 그리고 모든 주의 첫째 메시지, 모든 주의 둘째 메시지 순서(라운드 로빈)로 뽑아 기간 전체에서 고르게 고른다.
 * [MAX_SAMPLE_MESSAGES]개나 본문 [MAX_SAMPLE_CHARS]자를 넘기기 전에 멈추고, 뽑은 메시지는 시각순으로 놓는다.
 */
class SampleCandidates private constructor(private val values: List<ScoringMessage>) {
    fun pick(): ScoringSample {
        val ordered = inRoundRobinOrder()
        val withinChars = ordered.runningFold(0) { total, message -> total + message.length() }
            .drop(1)
            .count { it <= MAX_SAMPLE_CHARS }
        return ScoringSample(ordered.take(minOf(withinChars, MAX_SAMPLE_MESSAGES)).sortedBy { it.localTime })
    }

    private fun inRoundRobinOrder(): List<ScoringMessage> =
        values.groupBy { it.weekStart() }
            .flatMap { (week, messages) -> prioritized(messages).mapIndexed { round, it -> SampleSlot(round, week, it) } }
            .sortedWith(SampleSlot.ORDER)
            .map { it.message }

    private fun prioritized(messages: List<ScoringMessage>): List<ScoringMessage> =
        messages.sortedWith(compareByDescending<ScoringMessage> { isKeywordHit(it) }.thenBy { it.localTime })

    private fun isKeywordHit(message: ScoringMessage): Boolean = ScoreTheme.entries.any { message.mentions(it) }

    companion object {
        /** 표본 메시지 수의 상한. */
        const val MAX_SAMPLE_MESSAGES = 120

        /** 표본 본문 글자 수 합의 상한. */
        const val MAX_SAMPLE_CHARS = 6000

        /** 표본에 넣는 메시지 하나의 최대 글자 수. 더 긴 메시지는 자른다. */
        const val MAX_SAMPLE_TEXT_LENGTH = 200

        /** 아주 짧거나 웃음·이모지뿐인 메시지를 빼고, 긴 메시지는 [MAX_SAMPLE_TEXT_LENGTH]자로 자른다. */
        fun from(messages: List<ScoringMessage>): SampleCandidates = SampleCandidates(
            messages.filterNot { it.isLowSignal() }.map { it.truncated(MAX_SAMPLE_TEXT_LENGTH) },
        )
    }
}
