package com.santacut.domain.roast.model

/**
 * 작가에게 보낸 본인 줄의 말풍선 표시 목록. 상대 줄은 처음부터 들어 있지 않아 작가가 상대 줄 번호를 골라도 찾지 못한다.
 */
class LineQuotes(private val values: List<LineQuote>) {
    /** [lineId]번 본인 줄의 말풍선 표시. 보낸 본인 줄이 아니거나 지우고 나서 빈 줄이면 null이다. */
    fun find(lineId: Int): String? = values.find { quote -> quote.lineId == lineId }?.text?.takeIf(String::isNotBlank)

    /** 근거로 쓸 만한 말풍선 표시([QuoteText.isSubstantial])가 남은 줄 수. 근거 카드 슬롯 수의 상한이다. */
    fun usableCount(): Int = values.count { quote -> QuoteText.isSubstantial(quote.text) }

    companion object {
        val EMPTY = LineQuotes(emptyList())
    }
}
