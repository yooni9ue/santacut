package com.santacut.infrastructure.share.render

/**
 * 공개 공유 페이지의 리캡 카드 목록. 목차와 카드 HTML 조각을 만든다.
 * 스크립트가 꺼져 있어도 읽히도록 모든 카드를 본문에 그린다. 넘김 스크립트는 `data-` 속성만 읽고, 카드 글을 스크립트 안에 넣지 않는다.
 */
data class ShareSlides(private val values: List<ShareSlide>) {
    fun size(): Int = values.size

    /** 목차 항목(`<li>`)들. 각 항목은 카드 앵커로 가는 링크이고, 스크립트가 켜져 있으면 그 카드로 넘긴다. */
    fun tocHtml(): String = values.withIndex().joinToString(EMPTY) { (index, slide) -> slide.tocHtml(index) }

    /** 카드(`<article>`)들. 각 카드에 순서/전체 장수를 적는다. */
    fun cardsHtml(): String = values.withIndex().joinToString(EMPTY) { (index, slide) -> slide.cardHtml(index, values.size) }

    companion object {
        private const val EMPTY = ""
    }
}
