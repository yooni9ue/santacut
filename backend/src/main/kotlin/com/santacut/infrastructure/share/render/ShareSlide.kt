package com.santacut.infrastructure.share.render

/**
 * 공개 공유 페이지의 리캡 카드 한 장. 값은 HTML 이스케이프 전이고, 그릴 때 모두 [HtmlText.escape]로 이스케이프한다.
 *
 * @property kind 카드 종류 이름(VERDICT, ROAST 등). 화면 스타일을 고르는 데만 쓴다.
 * @property label 화면에 보일 카드 종류 이름(예: `판정`, `팩폭`)
 * @property scoreText 카드 상단의 총점 표시(예: `73.73`)
 * @property headline 카드 제목이나 본문 첫 줄
 * @property stamp 판정 카드의 도장 문구. 다른 카드는 null이다.
 * @property rows 제목 아래 글줄
 * @property bars 항목 막대 카드의 막대. 다른 카드는 비어 있다.
 */
data class ShareSlide(
    val kind: String,
    val label: String,
    val scoreText: String,
    val headline: String,
    val stamp: String? = null,
    val rows: ShareSlideRows = ShareSlideRows.NONE,
    val bars: ShareSlideBars = ShareSlideBars.NONE,
) {
    /** 목차 항목. [index]는 0부터다. */
    fun tocHtml(index: Int): String =
        "<li><a href=\"#${cardId(index)}\" data-jump=\"$index\">${index + 1}$TOC_SEPARATOR${HtmlText.escape(label)}</a></li>"

    /** 카드 한 장. 순서/전체 장수, 총점, 종류, (도장), 제목, 종류별 글줄과 막대를 그린다. */
    fun cardHtml(index: Int, total: Int): String {
        val position = "${index + 1}$POSITION_SEPARATOR$total"
        val escapedLabel = HtmlText.escape(label)
        return "<article class=\"card $KIND_CLASS_PREFIX${HtmlText.escape(kind.lowercase())}\" id=\"${cardId(index)}\" data-card tabindex=\"-1\" " +
            "aria-label=\"$position$LABEL_SEPARATOR$escapedLabel\">" +
            "<p class=\"card-top\"><span class=\"card-index\">$position</span>" +
            "<span class=\"card-score\">${HtmlText.escape(scoreText)}$SCORE_SUFFIX</span></p>" +
            "<p class=\"card-kind\">$escapedLabel</p>" +
            stamp?.let { "<span class=\"seal\">${HtmlText.escape(it)}</span>" }.orEmpty() +
            "<h3>${HtmlText.escape(headline)}</h3>" + rows.html() + bars.html() + "</article>"
    }

    private fun cardId(index: Int): String = "$CARD_ID_PREFIX${index + 1}"

    companion object {
        private const val CARD_ID_PREFIX = "card-"
        private const val KIND_CLASS_PREFIX = "card-"
        private const val POSITION_SEPARATOR = " / "
        private const val LABEL_SEPARATOR = " · "
        private const val TOC_SEPARATOR = ". "
        private const val SCORE_SUFFIX = "점"
    }
}
