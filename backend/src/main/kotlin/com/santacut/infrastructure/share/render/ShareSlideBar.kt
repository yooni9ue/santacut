package com.santacut.infrastructure.share.render

/**
 * 항목 막대 카드의 막대 하나.
 *
 * @property score 표시용 항목 점수(예: `71.67`)
 * @property percent 막대 길이(0~100). 서버가 정한 정수라 스타일 속성에 그대로 넣는다.
 */
data class ShareSlideBar(val label: String, val score: String, val percent: Int) {
    fun html(): String =
        "<li><span>${HtmlText.escape(label)}</span><span class=\"bar\"><span style=\"width:${percent.coerceIn(MIN_PERCENT, MAX_PERCENT)}%\"></span></span>" +
            "<span>${HtmlText.escape(score)}</span></li>"

    companion object {
        private const val MIN_PERCENT = 0
        private const val MAX_PERCENT = 100
    }
}
