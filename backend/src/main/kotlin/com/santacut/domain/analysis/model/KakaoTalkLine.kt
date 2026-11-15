package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.SeoulTime
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 카카오톡 TXT 내보내기의 한 줄. 시각은 서울 시각으로 읽는다.
 * 정규식은 공개된 내보내기 예시를 보고 만들었고 실제 샘플로는 아직 검증하지 않았다.
 */
class KakaoTalkLine(private val text: String) {
    /** PC 날짜 구분선이면 그 날짜. */
    fun pcDate(): LocalDate? {
        val (year, month, day) = PC_DATE_HEADER.matchEntire(text)?.destructured ?: return null
        return dateOrNull(year, month, day)
    }

    /** [date]에 쓴 PC 메시지 줄이면 그 메시지. */
    fun pcMessage(date: LocalDate): ChatMessage? {
        val (speaker, meridiem, hour, minute, body) = PC_MESSAGE.matchEntire(text)?.destructured ?: return null
        return messageOrNull(speaker, body) { date.atTime(hourOf(meridiem, hour), minute.toInt()) }
    }

    /** iOS나 Android 메시지 줄이면 그 메시지. */
    fun mobileMessage(): ChatMessage? {
        val match = IOS_MESSAGE.matchEntire(text) ?: ANDROID_MESSAGE.matchEntire(text) ?: return null
        val (year, month, day, meridiem, hour, minute, speaker, body) = match.destructured
        return messageOrNull(speaker, body) {
            LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hourOf(meridiem, hour), minute.toInt())
        }
    }

    /** 모바일 날짜 구분선이나, 시각 접두어는 있지만 `이름 : ` 없는 시스템 줄(입장·퇴장 안내 등)인지. */
    fun isMobileNonMessage(): Boolean =
        MOBILE_DATE_HEADER.matches(text) || ANDROID_TIME_HEADER.matches(text) ||
            IOS_PREFIX.containsMatchIn(text) || ANDROID_PREFIX.containsMatchIn(text)

    /** 메시지 줄이 아닌 줄은 바로 앞 메시지 본문의 다음 줄이다. */
    fun appendTo(message: ChatMessage): ChatMessage = message.continuedWith(text)

    private fun messageOrNull(speaker: String, body: String, dateTime: () -> LocalDateTime): ChatMessage? =
        try {
            ChatMessage(speaker, dateTime().atZone(SeoulTime.ZONE).toInstant(), body)
        } catch (exception: DateTimeException) {
            null
        }

    private fun dateOrNull(year: String, month: String, day: String): LocalDate? =
        try {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        } catch (exception: DateTimeException) {
            null
        }

    /** 오전 12시는 0시, 오후 12시는 12시, 그 밖의 오후는 12시간을 더한다. 오전·오후가 없으면 24시간 표기다. */
    private fun hourOf(meridiem: String, hour: String): Int = when (meridiem) {
        MORNING -> hour.toInt() % HALF_DAY_HOURS
        AFTERNOON -> hour.toInt() % HALF_DAY_HOURS + HALF_DAY_HOURS
        else -> hour.toInt()
    }

    companion object {
        private const val MORNING = "오전"
        private const val AFTERNOON = "오후"
        private const val HALF_DAY_HOURS = 12

        private val PC_DATE_HEADER = Regex("""-{5,} (\d{4})년 (\d{1,2})월 (\d{1,2})일 \S요일 -{5,}""")
        private val PC_MESSAGE = Regex("""\[(.{1,100}?)] \[(오전|오후) (\d{1,2}):(\d{2})] (.*)""")
        private val IOS_MESSAGE =
            Regex("""(\d{4})\. (\d{1,2})\. (\d{1,2})\. (?:(오전|오후) )?(\d{1,2}):(\d{2}), (.{1,100}?) : (.*)""")
        private val ANDROID_MESSAGE =
            Regex("""(\d{4})년 (\d{1,2})월 (\d{1,2})일 (오전|오후) (\d{1,2}):(\d{2}), (.{1,100}?) : (.*)""")
        private val MOBILE_DATE_HEADER = Regex("""\d{4}년 \d{1,2}월 \d{1,2}일 \S요일""")
        private val ANDROID_TIME_HEADER = Regex("""\d{4}년 \d{1,2}월 \d{1,2}일 (오전|오후) \d{1,2}:\d{2}""")
        private val IOS_PREFIX = Regex("""^\d{4}\. \d{1,2}\. \d{1,2}\. (?:(오전|오후) )?\d{1,2}:\d{2}, """)
        private val ANDROID_PREFIX = Regex("""^\d{4}년 \d{1,2}월 \d{1,2}일 (오전|오후) \d{1,2}:\d{2}, """)
    }
}
