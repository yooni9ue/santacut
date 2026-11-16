package com.santacut.infrastructure.share.render

/**
 * 공유 페이지 템플릿 자리표시자에 넣을 값. 모든 값은 이미 HTML 이스케이프를 마쳤다.
 */
class SharePageView private constructor(
    private val title: String,
    private val description: String,
    private val origin: String,
    private val token: String,
    private val comment: String,
    private val computedAt: String,
    private val stage: String,
    private val sharedAt: String,
    private val summary: String,
) {
    /** 자리표시자 이름에 해당하는 값. 모르는 이름은 빈 문자열로 바꾼다. */
    fun valueOf(placeholder: String): String = when (placeholder) {
        TITLE -> title
        DESCRIPTION -> description
        ORIGIN -> origin
        TOKEN -> token
        COMMENT -> comment
        COMPUTED_AT -> computedAt
        STAGE -> stage
        SHARED_AT -> sharedAt
        SUMMARY -> summary
        else -> EMPTY
    }

    companion object {
        private const val TITLE = "title"
        private const val DESCRIPTION = "description"
        private const val ORIGIN = "origin"
        private const val TOKEN = "token"
        private const val COMMENT = "comment"
        private const val COMPUTED_AT = "computedAt"
        private const val STAGE = "stage"
        private const val SHARED_AT = "sharedAt"
        private const val SUMMARY = "summary"
        private const val SUMMARY_SEPARATOR = " · "
        private const val EMPTY = ""

        fun of(card: ShareCard, publicOrigin: String): SharePageView = SharePageView(
            title = HtmlText.escape(listOfNotNull("${card.nickname} · 산타컷 ${card.score}점", card.archetype.ifEmpty { null }).joinToString(SUMMARY_SEPARATOR)),
            description = HtmlText.escape("${card.rank} · 선물 도착 예정 ${card.eta} · ${card.verdict}"),
            origin = HtmlText.escape(publicOrigin),
            token = card.token,
            comment = HtmlText.escape(card.comment),
            computedAt = HtmlText.escape(card.computedAt),
            stage = HtmlText.escape(card.stage),
            sharedAt = HtmlText.escape(card.sharedAt),
            summary = HtmlText.escape((card.quoteAndHighlights() + listOfNotNull(card.bars.ifEmpty { null })).joinToString(SUMMARY_SEPARATOR)),
        )

        /** 볼 수 없는 공유 결과 페이지용 값. 시작 링크에 쓸 공개 출처만 담는다. */
        fun inactive(publicOrigin: String): SharePageView = SharePageView(
            title = EMPTY,
            description = EMPTY,
            origin = HtmlText.escape(publicOrigin),
            token = EMPTY,
            comment = EMPTY,
            computedAt = EMPTY,
            stage = EMPTY,
            sharedAt = EMPTY,
            summary = EMPTY,
        )
    }
}
