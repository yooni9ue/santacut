package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierAct
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 작가에게 보내는 본인 말풍선 한 줄. 본문은 가린 뒤의 것이다.
 *
 * @property number 본인 말풍선 순번(1부터). 잘라 낸 뒤에도 원래 순번을 쓴다.
 * @property masked 이 줄에서 가린 원래 낱말(차단 목록용). 저장하지 않는다.
 */
class RoastLine(val number: Int, private val localTime: LocalDateTime, val text: String, val masked: List<String>) {
    fun act(): DossierAct = DossierAct.of(localTime.monthValue)

    /** `줄번호|월-일 시|본문`. 예: `12|01-16 21시|내일 ㄱ?`. */
    fun toInputLine(): String = "$number$SEPARATOR${localTime.format(MONTH_DAY)} ${localTime.hour}$HOUR_UNIT$SEPARATOR$text"

    fun inputLength(): Int = toInputLine().length

    companion object {
        private const val SEPARATOR = "|"
        private const val HOUR_UNIT = "시"
        private val MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd")

        /** 본인 메시지 [message]를 [number]번 줄로 만든다. 빈 본문이거나 통째로 뺄 줄([RoastRules.scrub])이면 null이다. */
        fun of(number: Int, message: ScoringMessage, rules: RoastRules): RoastLine? {
            val text = message.normalizedText()
            if (text.isEmpty()) return null
            val scrubbed = rules.scrub(text) ?: return null
            return RoastLine(number, message.localTime, scrubbed.text, scrubbed.masked)
        }
    }
}
