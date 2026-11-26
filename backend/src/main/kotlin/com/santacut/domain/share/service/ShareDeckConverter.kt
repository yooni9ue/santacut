package com.santacut.domain.share.service

import com.santacut.domain.share.model.RecapDeck
import com.santacut.domain.share.model.RecapDeckBar
import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind
import com.santacut.infrastructure.share.render.ShareDeck
import com.santacut.infrastructure.share.render.ShareSlide
import com.santacut.infrastructure.share.render.ShareSlideBar
import com.santacut.infrastructure.share.render.ShareSlideBars
import com.santacut.infrastructure.share.render.ShareSlideRow
import com.santacut.infrastructure.share.render.ShareSlideRows
import com.santacut.infrastructure.share.render.ShareSlides
import java.math.RoundingMode

/**
 * 저장한 리캡 카드 덱을 공개 페이지에 그릴 표시 값으로 바꾼다. 렌더러는 도메인 모델을 받지 않으므로 표시 문자열만 넘긴다.
 * 공개 페이지는 늘 기본(농담) 문구를 쓴다.
 */
object ShareDeckConverter {
    private const val GIFT_SKIN = "GIFT"
    private const val EMPTY = ""
    private const val GRADE_LABEL = "등급"
    private const val VERDICT_LABEL = "판결"
    private const val CUT_LABEL = "컷 거리"
    private const val RANK_LABEL = "전국 순위"
    private const val ETA_LABEL = "선물 도착 예정"
    private const val POINT_SUFFIX = "점"
    private const val CUT_OPEN = " (컷 "
    private const val CUT_CLOSE = "점)"
    private const val RANK_SUFFIX = "위"
    private const val RANKING_PENDING = "순위 반영 대기"
    private const val LETTER_HEADLINE = "산타가 남긴 쪽지"
    private const val STORY_HEADLINE = "세 막으로 본 올해"
    private const val ACT_SUFFIX = "막 · "
    private const val DIMENSIONS_HEADLINE = "네 항목 점수"

    fun of(deck: RecapDeck): ShareDeck = ShareDeck(deck.skin == GIFT_SKIN, ShareSlides(deck.cards.convertEach(::slideOf)))

    private fun slideOf(card: RecapDeckCard): ShareSlide = when (card.kind) {
        RecapDeckKind.VERDICT -> slide(card, card.title.orEmpty()).copy(stamp = card.stamp, rows = verdictRows(card))
        RecapDeckKind.ROAST -> slide(card, card.roast.orEmpty()).copy(rows = roastRows(card))
        RecapDeckKind.LETTER -> slide(card, LETTER_HEADLINE).copy(rows = textRows(card.letter))
        RecapDeckKind.STORY -> slide(card, STORY_HEADLINE).copy(rows = storyRows(card))
        RecapDeckKind.ARCHETYPE -> slide(card, card.archetypeName.orEmpty())
        RecapDeckKind.DIMENSIONS -> slide(card, DIMENSIONS_HEADLINE).copy(bars = ShareSlideBars(card.bars?.convertEach(::barOf).orEmpty()))
    }

    private fun slide(card: RecapDeckCard, headline: String): ShareSlide = ShareSlide(card.kind.name, card.kind.label, card.scoreText, headline)

    /** 등급, 판결, 컷 거리, 순위(표본이 모자라면 뺀다), 도착 예정일(석탄이거나 계산 전이면 뺀다). */
    private fun verdictRows(card: RecapDeckCard): ShareSlideRows = ShareSlideRows(
        listOfNotNull(
            card.grade?.let { ShareSlideRow(GRADE_LABEL, it) },
            card.verdict?.let { ShareSlideRow(VERDICT_LABEL, it) },
            card.cutDistance?.let { ShareSlideRow(CUT_LABEL, "$it$POINT_SUFFIX$CUT_OPEN${card.cut}$CUT_CLOSE") },
            rankText(card)?.let { ShareSlideRow(RANK_LABEL, it) },
            card.displayDeliveryEta()?.let { ShareSlideRow(ETA_LABEL, it) },
        ),
    )

    private fun rankText(card: RecapDeckCard): String? =
        card.rank?.let { "$it$RANK_SUFFIX" } ?: RANKING_PENDING.takeIf { card.rankingPending == true }

    /** 서버 숫자 칩(사실 없이 쓴 대체 짝이면 없다)과 뒤집기. */
    private fun roastRows(card: RecapDeckCard): ShareSlideRows {
        val chip = card.chipLabel?.let { label -> card.chipValue?.let { value -> ShareSlideRow(label, value) } }
        return ShareSlideRows(listOfNotNull(chip, card.flip?.let { ShareSlideRow(EMPTY, it) }))
    }

    private fun textRows(text: String?): ShareSlideRows = ShareSlideRows(listOfNotNull(text?.let { ShareSlideRow(EMPTY, it) }))

    private fun storyRows(card: RecapDeckCard): ShareSlideRows =
        ShareSlideRows(card.acts?.convertEach { act -> ShareSlideRow("${act.act}$ACT_SUFFIX${act.months}", act.line) }.orEmpty())

    private fun barOf(bar: RecapDeckBar): ShareSlideBar =
        ShareSlideBar(bar.label, bar.score.toPlainString(), bar.score.setScale(0, RoundingMode.HALF_UP).toInt())
}
