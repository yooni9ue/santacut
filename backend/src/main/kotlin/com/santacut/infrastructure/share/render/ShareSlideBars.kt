package com.santacut.infrastructure.share.render

/** 항목 막대 카드의 막대 목록. 비어 있으면 아무것도 그리지 않는다. */
data class ShareSlideBars(private val values: List<ShareSlideBar>) {
    fun html(): String = if (values.isEmpty()) EMPTY else "<ul class=\"bars\">${values.joinToString(EMPTY, transform = ShareSlideBar::html)}</ul>"

    companion object {
        private const val EMPTY = ""

        /** 막대가 없는 카드용. */
        val NONE = ShareSlideBars(emptyList())
    }
}
