package com.santacut.infrastructure.share.render

/** 리캡 카드 제목 아래 글줄 목록. 글은 모두 [HtmlText.escape]로 이스케이프한다. */
data class ShareSlideRows(private val values: List<ShareSlideRow>) {
    fun html(): String = values.joinToString(EMPTY, transform = ShareSlideRow::html)

    companion object {
        private const val EMPTY = ""

        /** 글줄이 없는 카드용. */
        val NONE = ShareSlideRows(emptyList())
    }
}
