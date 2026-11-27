package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierAct
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 작가에게 보내는 본인 말풍선 한 줄. 본문은 가린 뒤의 것이다.
 *
 * @property number 본인 말풍선 순번(1부터). 잘라 낸 뒤에도 원래 순번을 쓴다.
 * @property masked 이 줄에서 가린 원래 낱말(차단 목록용). 저장하지 않는다.
 * @property names 이 줄의 호칭·부르는 말에서 떼어 낸 이름 줄기. 모든 줄을 다시 가리는 데 쓴다.
 * @property original 가리기 전 정규화 본문. 근거 카드 말풍선 표시([QuoteText])를 만들 때만 쓰고 작가에게 보내거나 저장하지 않는다.
 * @property burst 본인 말풍선끼리 3분 안으로 이어 보낸 묶음 안의 줄인지(연달아 보내기 칩의 말풍선 조건)
 */
class RoastLine(
    val number: Int,
    private val localTime: LocalDateTime,
    val text: String,
    val masked: List<String>,
    val names: List<String> = emptyList(),
    private val original: String = text,
    private val burst: Boolean = false,
) {
    fun act(): DossierAct = DossierAct.of(localTime.monthValue)

    /**
     * `줄번호|월-일 요일 시|본문`. 연달아 보낸 묶음 안의 줄이면 시 뒤에 [BURST_MARK]를 붙인다. 예: `12|01-16 금 21시|내일 ㄱ?`, `13|01-16 금 21시 연달아|빨리`.
     * 요일과 연달아 표시는 작가가 주말·연달아 보내기 칩에 맞는 말풍선을 고르게 하려는 것이다.
     */
    fun toInputLine(): String {
        val burstMark = if (burst) " $BURST_MARK" else ""
        return "$number$SEPARATOR${localTime.format(MONTH_DAY_WEEKDAY)} ${localTime.hour}$HOUR_UNIT$burstMark$SEPARATOR$text"
    }

    fun inputLength(): Int = toInputLine().length

    /** 다른 줄에서 찾은 이름 줄기 패턴 [pattern]으로 본문을 다시 가린 줄. '현규 왔냐'처럼 호칭 없이 나온 이름도 가린다. */
    fun remaskedBy(pattern: MaskPattern): RoastLine {
        val found = mutableListOf<String>()
        val remasked = pattern.maskIn(text, found)
        return RoastLine(number, localTime, remasked, masked + found, names, original, burst)
    }

    /** 말풍선 표시에서 ○○로 가릴 낱말. 가린 한글 낱말(사람 이름, 사는 곳 이름)과 이름 줄기다. 영문은 링크와 함께 지우므로 넣지 않는다. */
    fun quoteWords(): List<String> = masked.filter { word -> HANGUL.containsMatchIn(word) } + names

    /** 이름 패턴 [names]로 가린 이 줄의 말풍선 표시. */
    fun quoteBy(names: MaskPattern?): LineQuote = LineQuote(number, QuoteText.render(original, names), localTime, burst)

    companion object {
        private const val SEPARATOR = "|"
        private const val HOUR_UNIT = "시"
        private const val BURST_MARK = "연달아"
        private val MONTH_DAY_WEEKDAY = DateTimeFormatter.ofPattern("MM-dd E", Locale.KOREAN)
        private val HANGUL = Regex("[가-힣]")

        /** 본인 메시지 [message]를 [number]번 줄로 만든다. [burst]는 연달아 보낸 묶음 안인지다. 빈 본문이면 null이다. */
        fun of(number: Int, message: ScoringMessage, rules: RoastRules, burst: Boolean = false): RoastLine? {
            val text = message.normalizedText()
            if (text.isEmpty()) return null
            val scrubbed = rules.scrub(text)
            return RoastLine(number, message.localTime, scrubbed.text, scrubbed.masked, scrubbed.names, text, burst)
        }
    }
}
