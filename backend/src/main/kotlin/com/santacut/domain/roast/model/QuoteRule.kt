package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.dossier.model.KindWords
import java.time.DayOfWeek

/**
 * 지표 칩과 말풍선이 같은 이야기를 하는지 가리는 규칙(roast/facts.json의 quote_rule). 근거 카드는 칩 하나와 말풍선 하나를 나란히 띄우므로,
 * 말풍선은 그 칩이 센 버릇을 직접 보여 줘야 한다. 물음표 칩에 물음표 없는 말풍선, 피크타임 칩에 다른 시각의 말풍선은 떨어진다.
 * 말풍선으로 보여 줄 수 없는 지표(이모지·사진, 양, 길이, 짧은 말풍선 비율, 답장 간격 등)는 [NONE]이라 말풍선 없이 칩만 크게 띄운다.
 * 짧은 말풍선 비율은 근거 말풍선이 한글 네 음절 이상이어야 해 다섯 글자 이하 조건과 거의 겹치지 않으므로 [NONE]으로 둔다.
 * 시간대 경계는 조서 집계와 같다(새벽 0~4시, 아침 6~8시, 근무시간 9~17시, 연달아 보내기는 본인 말풍선끼리 3분 안).
 *
 * @property hint 작가 입력의 칩 후보에 싣는 말풍선 조건 설명([PEAK_HOUR]는 [HOUR_SLOT]에 가장 바쁜 시를 넣는다)
 */
enum class QuoteRule(val hint: String) {
    /** 입버릇 칩. 말풍선에 그 입버릇이 있어야 한다([RoastBrief.quoteHasPhrase]가 사전 표면형으로 본다). */
    @JsonProperty("phrase")
    PHRASE("칩 입버릇이 든 말풍선만"),

    @JsonProperty("question")
    QUESTION("물음표가 든 말풍선만"),

    @JsonProperty("exclaim")
    EXCLAIM("느낌표가 든 말풍선만"),

    @JsonProperty("laugh")
    LAUGH("ㅋ이나 ㅎ이 든 말풍선만"),

    @JsonProperty("night")
    NIGHT("새벽(자정부터 다섯 시 전)에 보낸 말풍선만"),

    @JsonProperty("morning")
    MORNING("아침(여섯 시부터 아홉 시 전)에 보낸 말풍선만"),

    @JsonProperty("office")
    OFFICE("근무시간(아홉 시부터 여섯 시 전)에 보낸 말풍선만"),

    @JsonProperty("peak_hour")
    PEAK_HOUR("가장 바쁜 시({hour})에 보낸 말풍선만"),

    @JsonProperty("weekend")
    WEEKEND("토요일·일요일에 보낸 말풍선만"),

    @JsonProperty("burst")
    BURST("연달아 보낸 묶음 안의 말풍선만(transcript 시각 뒤 '연달아' 표시)"),

    @JsonProperty("kind")
    KIND("고맙다·수고 같은 따뜻한 말이 든 말풍선만"),

    /** 말풍선으로 보여 줄 수 없는 지표. 근거 카드는 line_id를 null로 두고 칩만 크게 띄운다. */
    @JsonProperty("none")
    NONE("말풍선 없이 칩만 쓴다(line_id는 null)"),
    ;

    /** 작가 입력에 싣는 조건 설명. 가장 바쁜 시 [peakHour]를 모르면 [PEAK_HOUR]는 말풍선 없이 쓰라고 한다. */
    fun hintWith(peakHour: Int?): String = when {
        this != PEAK_HOUR -> hint
        peakHour == null -> NONE.hint
        else -> hint.replace(HOUR_SLOT, "$peakHour$HOUR_UNIT")
    }

    /**
     * 말풍선 [quote]가 이 칩의 버릇을 보여 주는지. 글자 조건은 카드에 실제로 뜨는 말풍선 표시로 본다(잘려 물음표가 사라지면 떨어진다).
     * 시각 조건은 그 줄을 보낸 한국 시각으로 본다. [PHRASE]는 여기서 보지 않고 늘 참이고, [NONE]은 늘 거짓이다.
     */
    fun accepts(quote: LineQuote, peakHour: Int?, kinds: KindWords?): Boolean {
        val text = quote.text
        val time = quote.localTime
        return when (this) {
            PHRASE -> true
            QUESTION -> QUESTION_MARKS.containsMatchIn(text)
            EXCLAIM -> EXCLAIM_MARKS.containsMatchIn(text)
            LAUGH -> LAUGH_MARKS.containsMatchIn(text)
            NIGHT -> time != null && time.hour in NIGHT_HOURS
            MORNING -> time != null && time.hour in MORNING_HOURS
            OFFICE -> time != null && time.hour in OFFICE_HOURS
            PEAK_HOUR -> time != null && peakHour != null && time.hour == peakHour
            WEEKEND -> time != null && time.dayOfWeek in WEEKEND_DAYS
            BURST -> quote.burst
            KIND -> kinds?.appearIn(text) == true
            NONE -> false
        }
    }

    companion object {
        private const val HOUR_SLOT = "{hour}"
        private const val HOUR_UNIT = "시"
        private val QUESTION_MARKS = Regex("[?？]")
        private val EXCLAIM_MARKS = Regex("[!！]")
        private val LAUGH_MARKS = Regex("[ㅋㅎ]")
        private val NIGHT_HOURS = 0..4
        private val MORNING_HOURS = 6..8
        private val OFFICE_HOURS = 9..17
        private val WEEKEND_DAYS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}
