package com.santacut.domain.roast.model

import java.time.LocalDateTime

/**
 * 작가에게 보낸 본인 줄 하나([lineId]번)의 말풍선 표시([QuoteText]). 저장하는 것은 근거로 고른 줄의 [text]뿐이다.
 * 보낸 시각과 연달아 보내기 여부는 칩과 말풍선이 맞는지([QuoteRule]) 볼 때만 쓰고 저장하지 않는다.
 *
 * @property localTime 그 줄을 보낸 한국 시각. 모르면 null이고 시각 조건 칩에는 맞지 않는 것으로 본다.
 * @property burst 본인 말풍선끼리 3분 안으로 이어 보낸 묶음 안의 줄인지
 */
data class LineQuote(val lineId: Int, val text: String, val localTime: LocalDateTime? = null, val burst: Boolean = false)
