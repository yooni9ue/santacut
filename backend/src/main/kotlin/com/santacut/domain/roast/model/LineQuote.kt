package com.santacut.domain.roast.model

/** 작가에게 보낸 본인 줄 하나([lineId]번)의 말풍선 표시([QuoteText]). 저장하는 것은 근거로 고른 줄의 [text]뿐이다. */
data class LineQuote(val lineId: Int, val text: String)
