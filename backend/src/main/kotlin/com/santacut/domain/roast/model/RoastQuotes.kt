package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.PhraseTally

/**
 * 작가가 작은따옴표로 인용해도 되는 본인 입버릇(사전 표기 원형). 비속어·사람 부르는 말·긴 말·영문·숫자는 뺀다. JSON에서는 배열 하나로 나간다.
 * 목록 밖 웃음·울음 표기 인용은 [isLaughOrCry]로 가리고, 본인 말풍선에 실제로 있는지는 [RoastBrief.isQuotable]이 본다.
 */
class RoastQuotes(private val values: List<String>) {
    @JsonValue
    fun toList(): List<String> = values.toList()

    operator fun contains(quote: String): Boolean = quote in values

    companion object {
        val EMPTY = RoastQuotes(emptyList())
        private const val MAX_QUOTES = 8

        /** 웃음(ㅋ, ㅎ)이나 울음(ㅠ, ㅜ) 자모만 두 번에서 여섯 번 되풀이한 표기. */
        private val LAUGH_OR_CRY = Regex("[ㅋㅎ]{2,6}|[ㅠㅜ]{2,6}")

        /**
         * 웃음(ㅋ, ㅎ)이나 울음(ㅠ, ㅜ) 자모만 두 번에서 여섯 번 되풀이한 표기인지. 이름·숫자·비속어가 들어갈 수 없어 공유해도 안전하다.
         * 'ㅅㅂ' 같은 자모 비속어는 아니다.
         */
        fun isLaughOrCry(quote: String): Boolean = LAUGH_OR_CRY.matches(quote)

        /** 조서 집계 [stats](지금 사전으로 다시 읽은 것)의 많이 쓴 말과 특이한 말에서 고른다. */
        fun of(stats: DossierStats, rules: RoastRules): RoastQuotes = RoastQuotes(
            (stats.words.phrasesFrequent.toList() + stats.words.phrasesDistinct.toList())
                .distinctBy(PhraseTally::key)
                .filter { tally -> rules.isQuotable(tally.key, tally.display) }
                .map(PhraseTally::display)
                .distinct()
                .take(MAX_QUOTES),
        )
    }
}
