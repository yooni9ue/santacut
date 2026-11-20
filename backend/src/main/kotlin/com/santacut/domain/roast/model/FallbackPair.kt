package com.santacut.domain.roast.model

/** 대체 짝 문장 하나(roast/fallback.json). 입버릇 짝은 `{quote}`에 그 입버릇 표기를 넣는다. */
data class FallbackPair(val roast: String, val flip: String) {
    /** [factKey] 사실의 대체 짝. [quote]가 있으면 `{quote}`를 그 표기로 바꾼다. */
    fun toPair(factKey: String, quote: String?): RoastPair = RoastPair(
        factKey, filled(roast, quote), filled(flip, quote), RoastSource.FALLBACK,
    )

    private fun filled(text: String, quote: String?): String = text.replace(PhraseFactSpec.QUOTE, quote.orEmpty())
}
