package com.santacut.domain.analysis.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * 채점용 신호와 표본을 만들 때 쓰는 본인 메시지 하나. [localTime]은 한국 시각이다.
 */
data class ScoringMessage(
    val localTime: LocalDateTime,
    val text: String,
) {
    /** 이 메시지가 든 주의 월요일. 표본은 이 달력 주 단위로 고르게 뽑는다. */
    fun weekStart(): LocalDate = localTime.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 공백, 문장 부호, 기호·이모지, ㅋ·ㅎ·ㅠ·ㅜ를 빼면 [MIN_MEANINGFUL_LENGTH]자보다 짧은 메시지. */
    fun isLowSignal(): Boolean = NOISE.replace(text, "").length < MIN_MEANINGFUL_LENGTH

    fun mentions(theme: ScoreTheme): Boolean = theme.isMentionedIn(text)

    fun truncated(maxLength: Int): ScoringMessage = copy(text = text.take(maxLength))

    fun length(): Int = text.length

    companion object {
        private const val MIN_MEANINGFUL_LENGTH = 2
        private val NOISE = Regex("[\\s\\p{P}\\p{S}\\p{M}\\p{Cf}ㅋㅎㅠㅜ]")
    }
}
