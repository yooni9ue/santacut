package com.santacut.domain.analysis.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * 채점용 통계와 표본을 만들 때 쓰는 본인 메시지 하나. [localTime]은 한국 시각이다.
 */
data class ScoringMessage(
    val localTime: LocalDateTime,
    val text: String,
) {
    fun date(): LocalDate = localTime.toLocalDate()

    /** 이 메시지가 든 주의 월요일. 표본은 이 달력 주 단위로 고르게 뽑는다. */
    fun weekStart(): LocalDate = date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun hourBucket(): HourBucket = HourBucket.of(localTime.hour)

    /** 웃음(ㅋㅋ, ㅎㅎ), 울음(ㅠㅠ), 이모지 가운데 하나라도 들어 있는지. */
    fun hasLaughOrEmoji(): Boolean = LAUGH_OR_EMOJI.containsMatchIn(text)

    /** 공백, 문장 부호, 기호·이모지, ㅋ·ㅎ·ㅠ·ㅜ를 빼면 [MIN_MEANINGFUL_LENGTH]자보다 짧은 메시지. */
    fun isLowSignal(): Boolean = NOISE.replace(text, "").length < MIN_MEANINGFUL_LENGTH

    fun mentions(theme: ScoreTheme): Boolean = theme.isMentionedIn(text)

    fun truncated(maxLength: Int): ScoringMessage = copy(text = text.take(maxLength))

    fun length(): Int = text.length

    companion object {
        private const val MIN_MEANINGFUL_LENGTH = 2
        private val LAUGH_OR_EMOJI = Regex("[ㅋㅎ]{2,}|[ㅠㅜ]{2,}|\\p{So}")
        private val NOISE = Regex("[\\s\\p{P}\\p{S}\\p{M}\\p{Cf}ㅋㅎㅠㅜ]")
    }
}
