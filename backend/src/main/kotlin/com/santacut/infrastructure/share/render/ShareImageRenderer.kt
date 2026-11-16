package com.santacut.infrastructure.share.render

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * 공유 결과를 1200×630 PNG 카드로 그린다.
 */
@Component
class ShareImageRenderer {
    fun render(card: ShareCard): ByteArray {
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.color = BACKGROUND_COLOR
            graphics.fillRect(0, 0, WIDTH, HEIGHT)
            drawText(graphics, card)
        } finally {
            graphics.dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(image, PNG_FORMAT, it) }.toByteArray()
    }

    private fun drawText(graphics: Graphics2D, card: ShareCard) {
        graphics.color = HEADLINE_COLOR
        drawLine(graphics, ShareCardLine.HEADLINE, if (card.archetype.isEmpty()) HEADLINE else "$HEADLINE_PREFIX${card.archetype}")
        graphics.color = TEXT_COLOR
        drawLine(graphics, ShareCardLine.NICKNAME_SCORE, "${card.nickname}  ${card.score}점")
        drawLine(graphics, ShareCardLine.RANK_VERDICT, "${card.rank} · ${card.verdict}".take(MAX_VERDICT_LENGTH))
        drawLine(graphics, ShareCardLine.ETA, "선물 도착 예정: ${card.eta}")
        drawLine(graphics, ShareCardLine.COMMENT, summaryOf(card).take(MAX_COMMENT_LENGTH))
        drawLine(graphics, ShareCardLine.META, metaOf(card).take(MAX_META_LENGTH))
        drawLine(graphics, ShareCardLine.FOOTER, FOOTER)
    }

    /** 배지·상위 %·약한 항목 줄. 본인이 고른 한마디가 있으면 앞에 붙이고, 둘 다 없으면 재미용 한마디다. */
    private fun summaryOf(card: ShareCard): String {
        val quoted = card.quote?.let { "“$it”" }
        val parts = listOfNotNull(quoted, card.highlights.ifEmpty { null })
        return if (parts.isEmpty()) card.comment else parts.joinToString(SEPARATOR)
    }

    /** 항목 막대 요약과 순위 상태. 예전 링크처럼 막대가 없으면 계산 시각과 상태다. */
    private fun metaOf(card: ShareCard): String =
        if (card.bars.isEmpty()) "계산 ${card.computedAt} · ${card.stage}" else "${card.bars} · ${card.stage}"

    private fun drawLine(graphics: Graphics2D, line: ShareCardLine, text: String) {
        graphics.font = Font(FONT_FAMILY, line.style, line.size)
        graphics.drawString(text, LEFT_MARGIN, line.baseline)
    }

    companion object {
        private const val WIDTH = 1200
        private const val HEIGHT = 630
        private const val LEFT_MARGIN = 60
        private const val PNG_FORMAT = "png"
        private const val FONT_FAMILY = "Noto Sans CJK KR"
        private const val HEADLINE = "산타컷 · 너 올해 통과 가능?"
        private const val FOOTER = "산타컷 AI 심사 · 재미를 위한 결과"
        private const val MAX_VERDICT_LENGTH = 40
        private const val MAX_COMMENT_LENGTH = 42
        private const val MAX_META_LENGTH = 56
        private const val HEADLINE_PREFIX = "산타컷 · "
        private const val SEPARATOR = " · "
        private val BACKGROUND_COLOR = Color(255, 247, 237)
        private val HEADLINE_COLOR = Color(185, 28, 28)
        private val TEXT_COLOR = Color(67, 40, 24)
    }
}
