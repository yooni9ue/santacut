package com.santacut.infrastructure.share.render

/** 리캡 카드 제목 아래 글줄 하나. [label]이 비어 있으면 본문 [text]만 보인다. 값은 HTML 이스케이프 전이다. */
data class ShareSlideRow(val label: String, val text: String) {
    fun html(): String =
        if (label.isEmpty()) {
            "<p class=\"card-text\">${HtmlText.escape(text)}</p>"
        } else {
            "<p class=\"card-row\"><b>${HtmlText.escape(label)}</b> ${HtmlText.escape(text)}</p>"
        }
}
