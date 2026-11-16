package com.santacut.infrastructure.analysis.client

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * AI 모델에 보내는 표본 메시지 한 줄. 모델은 [index]를 근거 번호로 그대로 돌려준다.
 * 시각은 보내지 않는다. 채점 프롬프트가 활동 시간대를 점수에 쓰지 못하게 하므로 쓰이지 않는 토큰이다.
 *
 * @property weekStart 메시지가 든 주의 월요일. 주가 바뀔 때만 머리줄로 쓴다.
 */
class ScoreModelEvent(
    val index: Int,
    val weekStart: LocalDate,
    val text: String,
) {
    /** `번호|본문` 한 줄. 본문의 줄바꿈은 공백으로 바꿔 메시지 하나가 한 줄이 되게 한다. */
    fun toLine(): String = "$index$SEPARATOR${LINE_BREAKS.replace(text, LINE_BREAK_REPLACEMENT)}"

    /** `# 11-09주`처럼 주 시작 날짜를 적은 머리줄. */
    fun weekHeader(): String = "$WEEK_HEADER_PREFIX${WEEK_FORMAT.format(weekStart)}$WEEK_HEADER_SUFFIX"

    fun isInSameWeekAs(other: ScoreModelEvent): Boolean = weekStart == other.weekStart

    companion object {
        private const val SEPARATOR = "|"
        private const val WEEK_HEADER_PREFIX = "# "
        private const val WEEK_HEADER_SUFFIX = "주"
        private val WEEK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")
        private val LINE_BREAKS = Regex("\\R")
        private const val LINE_BREAK_REPLACEMENT = " "
    }
}
