package com.santacut.infrastructure.share.render

import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ShareImageRendererTest {
    private val renderer = ShareImageRenderer()

    @Test
    fun `같은 카드는 다시 그리지 않고 기억한 PNG를 돌려준다`() {
        val card = card("token-1", null)

        assertSame(renderer.render(card), renderer.render(card.copy()))
    }

    @Test
    fun `한마디가 바뀌면 새로 그린다`() {
        assertNotSame(renderer.render(card("token-1", null)), renderer.render(card("token-1", "한마디")))
    }

    @Test
    fun `기억할 수 있는 장수를 넘으면 가장 오래 안 쓴 카드부터 잊는다`() {
        val first = card("token-0", null)
        val firstImage = renderer.render(first)
        (1..ShareImageRenderer.MAX_CACHED_IMAGES).forEach { renderer.render(card("token-$it", null)) }

        assertNotSame(firstImage, renderer.render(first))
    }

    private fun card(token: String, quote: String?): ShareCard = ShareCard(
        token, "산타", "82.45", "3위", "통과", "12월 24일", "한마디 요약", "12월 1일", "미리보기", "12월 1일", quote = quote,
    )
}
