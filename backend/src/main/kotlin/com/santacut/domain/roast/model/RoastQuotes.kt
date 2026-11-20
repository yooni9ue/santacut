package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.PhraseTally

/**
 * 작가가 작은따옴표로 인용해도 되는 본인 입버릇(사전 표기 원형). 비속어·사람 부르는 말·긴 말·영문·숫자는 뺀다. JSON에서는 배열 하나로 나간다.
 * 목록 밖이어도 웃음·울음 자모만 되풀이한 표기('ㅋㅋㅋ', 'ㅎㅎ', 'ㅠㅠ')는 인용으로 받는다. 이름·숫자·비속어가 들어갈 수 없어
 * 공유해도 안전한데, 작가가 목록에 없다는 이유로 자주 떨어뜨려 다시 쓰기만 늘렸기 때문이다. 'ㅅㅂ' 같은 자모 비속어는 받지 않는다.
 */
class RoastQuotes(private val values: List<String>) {
    @JsonValue
    fun toList(): List<String> = values.toList()

    operator fun contains(quote: String): Boolean = quote in values || LAUGH_OR_CRY.matches(quote)

    companion object {
        val EMPTY = RoastQuotes(emptyList())
        private const val MAX_QUOTES = 8

        /** 웃음(ㅋ, ㅎ)이나 울음(ㅠ, ㅜ) 자모만 두 번에서 여섯 번 되풀이한 표기. */
        private val LAUGH_OR_CRY = Regex("[ㅋㅎ]{2,6}|[ㅠㅜ]{2,6}")

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
