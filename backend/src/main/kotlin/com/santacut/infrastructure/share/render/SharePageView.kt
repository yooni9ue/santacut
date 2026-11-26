package com.santacut.infrastructure.share.render

/**
 * 공유 페이지 템플릿 자리표시자에 넣을 값. 모든 값은 이미 HTML 이스케이프를 마쳤다.
 * 카드 덱 페이지의 [script]와 [santa]는 데이터가 섞이지 않은 고정 리소스라 그대로 넣는다.
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
    private val cardCount: String = EMPTY,
    private val toc: String = EMPTY,
    private val cards: String = EMPTY,
    private val skin: String = EMPTY,
    private val santa: String = EMPTY,
    private val script: String = EMPTY,
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
        CARD_COUNT -> cardCount
        TOC -> toc
        CARDS -> cards
        SKIN -> skin
        SANTA -> santa
        SCRIPT -> script
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
        private const val CARD_COUNT = "cardCount"
        private const val TOC = "toc"
        private const val CARDS = "cards"
        private const val SKIN = "skin"
        private const val SANTA = "santa"
        private const val SCRIPT = "script"
        private const val GIFT_SKIN = "skin-gift"
        private const val COAL_SKIN = "skin-coal"
        private const val SUMMARY_SEPARATOR = " · "
        private const val EMPTY = ""

        /** 요약 페이지 값에 리캡 카드 덱 [deck]의 목차·카드 HTML과 고정 리소스 [santa], [script]를 더한다. */
        fun deck(card: ShareCard, deck: ShareDeck, publicOrigin: String, santa: String, script: String): SharePageView {
            val summary = of(card, publicOrigin)
            return SharePageView(
                summary.title, summary.description, summary.origin, summary.token, summary.comment, summary.computedAt,
                summary.stage, summary.sharedAt, summary.summary, deck.slides.size().toString(), deck.slides.tocHtml(),
                deck.slides.cardsHtml(), if (deck.gift) GIFT_SKIN else COAL_SKIN, santa, script,
            )
        }

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
