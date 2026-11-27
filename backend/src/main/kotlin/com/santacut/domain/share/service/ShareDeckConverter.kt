package com.santacut.domain.share.service

import com.santacut.domain.share.model.RecapDeck
import com.santacut.domain.share.model.RecapDeckBand
import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind
import com.santacut.infrastructure.share.render.ShareDeck
import com.santacut.infrastructure.share.render.ShareSlide
import com.santacut.infrastructure.share.render.ShareSlideRow
import com.santacut.infrastructure.share.render.ShareSlideRows
import com.santacut.infrastructure.share.render.ShareSlides

/**
 * 저장 형식 2판 리캡 카드 덱을 공개 페이지에 그릴 표시 값으로 바꾼다. 렌더러는 도메인 모델을 받지 않으므로 표시 문자열만 넘긴다.
 * 모든 카드 상단에 순위 바(`전국 N등 · 상위 P%`)를 붙인다. 1판 덱은 [ShareDeckV1Converter]가 그린다.
 */
object ShareDeckConverter {
    private const val EMPTY = ""
    private const val GRADE_LABEL = "구간"
    private const val CUT_LABEL = "컷 거리"
    private const val ETA_LABEL = "선물 도착 예정"
    private const val QUOTE_LABEL = "본인 말"
    private const val CHIP_LABEL = "칩"
    private const val TITLE_LABEL = "칭호"
    private const val ARCHETYPE_LABEL = "산타 유형"
    private const val POINT_SUFFIX = "점"
    private const val ORDINAL_SEPARATOR = " "
    private const val CURRENT_BAND = "◀ 여기"
    private const val QUOTE_OPEN = "“"
    private const val QUOTE_CLOSE = "”"
    private const val HASH = "#산타컷 #"
    private const val RANK_PREFIX = "전국 "
    private const val RANK_SUFFIX = "등"
    private const val TOP_SEPARATOR = " · 상위 "
    private const val PERCENT = "%"

    fun of(deck: RecapDeck): ShareDeck = ShareDeck(deck.isGift(), ShareSlides(deck.cards.convertEach(::slideOf)))

    private fun slideOf(card: RecapDeckCard): ShareSlide = when (card.kind) {
        RecapDeckKind.VERDICT -> slide(card, card.verdict.orEmpty()).copy(rows = verdictRows(card))
        RecapDeckKind.CUTLINE -> slide(card, card.grade.orEmpty()).copy(rows = cutlineRows(card))
        RecapDeckKind.REASON, RecapDeckKind.CUT, RecapDeckKind.KEEP ->
            slide(card, card.label.orEmpty()).copy(label = card.kind.label + ORDINAL_SEPARATOR + card.ordinal, rows = evidenceRows(card))
        RecapDeckKind.CONCLUSION -> slide(card, card.grade.orEmpty()).copy(rows = conclusionRows(card))
    }

    private fun slide(card: RecapDeckCard, headline: String): ShareSlide =
        ShareSlide(card.kind.name, card.kind.label, card.scoreText, headline, topBar = topBarOf(card))

    /** `전국 N등 · 상위 P%`. 상위 비율이 없으면(참가자 100명 미만) `전국 N등`만 쓰고, 공유할 때 순위가 없었으면 null이다. */
    private fun topBarOf(card: RecapDeckCard): String? {
        val rank = card.rank ?: return null
        val percent = card.topPercent?.let { value -> "$TOP_SEPARATOR$value$PERCENT" }.orEmpty()
        return "$RANK_PREFIX$rank$RANK_SUFFIX$percent"
    }

    private fun verdictRows(card: RecapDeckCard): ShareSlideRows = ShareSlideRows(
        listOfNotNull(
            card.verdictLine?.let { ShareSlideRow(EMPTY, it) },
            card.grade?.let { ShareSlideRow(GRADE_LABEL, it) },
            card.cutDistance?.let { ShareSlideRow(CUT_LABEL, it + POINT_SUFFIX) },
            card.displayDeliveryEta()?.let { ShareSlideRow(ETA_LABEL, it) },
        ),
    )

    private fun cutlineRows(card: RecapDeckCard): ShareSlideRows = ShareSlideRows(
        listOfNotNull(card.bandDetail?.let { ShareSlideRow(EMPTY, it) }) + card.bands?.convertEach(::bandRow).orEmpty(),
    )

    private fun bandRow(band: RecapDeckBand): ShareSlideRow =
        ShareSlideRow(band.range, if (band.current) "${band.label} $CURRENT_BAND" else band.label)

    private fun evidenceRows(card: RecapDeckCard): ShareSlideRows = ShareSlideRows(
        listOfNotNull(
            card.quote?.let { ShareSlideRow(QUOTE_LABEL, QUOTE_OPEN + it + QUOTE_CLOSE) },
            card.chipText?.let { ShareSlideRow(CHIP_LABEL, it) },
            card.line?.let { ShareSlideRow(EMPTY, it) },
            card.dimTag?.let { ShareSlideRow(EMPTY, it) },
        ),
    )

    private fun conclusionRows(card: RecapDeckCard): ShareSlideRows = ShareSlideRows(
        listOfNotNull(
            card.title?.let { ShareSlideRow(TITLE_LABEL, it) },
            card.archetypeName?.let { ShareSlideRow(ARCHETYPE_LABEL, it) },
            card.heroQuote?.let { ShareSlideRow(QUOTE_LABEL, QUOTE_OPEN + it + QUOTE_CLOSE) },
            card.heroChipText?.let { ShareSlideRow(CHIP_LABEL, it) },
            card.finalLine?.let { ShareSlideRow(EMPTY, it) },
            card.friendCall?.let { call -> ShareSlideRow(EMPTY, call + ORDINAL_SEPARATOR + HASH + card.bandTag.orEmpty()) },
        ),
    )
}
