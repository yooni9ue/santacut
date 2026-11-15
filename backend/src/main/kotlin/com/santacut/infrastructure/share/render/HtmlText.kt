package com.santacut.infrastructure.share.render

/**
 * HTML 본문과 속성 값에 넣을 텍스트를 이스케이프한다.
 */
object HtmlText {
    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
