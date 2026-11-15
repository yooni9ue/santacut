package com.santacut.infrastructure.render

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharePageRendererTest {
    private val card = ShareCard(
        "T".repeat(43), "산타도와줘", "82.45", 3, "선물 배송 출발",
        "2026년 12월 25일 09:03", "<script>alert('x')</script>", "2026-12-01 01:02:03.004",
    )

    @Test
    fun `공유 문구는 실행 가능한 텍스트와 속성 값을 이스케이프한다`() {
        assertEquals("&lt;script&gt;&amp;&quot;&#39;", HtmlText.escape("<script>&\"'"))
    }

    @Test
    fun `공유 페이지는 기존과 같은 제목과 Open Graph 메타데이터를 그린다`() {
        val token = card.token
        assertContainsAll(
            renderPage(),
            "<title>산타도와줘 · 산타컷 82.45점</title>",
            "<meta property=\"og:description\" content=\"3위 · 선물 도착 예정 2026년 12월 25일 09:03 · 선물 배송 출발\">",
            "<meta property=\"og:url\" content=\"https://santacut.example/s/$token\">",
            "<meta property=\"og:image\" content=\"https://santacut.example/s/$token/image.png\">",
        )
    }

    @Test
    fun `공유 페이지 본문은 한마디를 이스케이프하고 계산 시각을 UTC로 보여 준다`() {
        assertContainsAll(
            renderPage(),
            "<p>&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;</p>",
            "<p>계산 시각: 2026-12-01 01:02:03.004 UTC</p>",
        )
    }

    @Test
    fun `공유 페이지는 기존과 같은 문서 시작과 끝을 가지고 치환되지 않은 자리 표시자가 없다`() {
        val html = renderPage()
        assertTrue(html.startsWith("<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"))
        assertTrue(html.endsWith("<a href=\"https://santacut.example\">나도 산타컷 도전하기</a></body></html>"))
        assertFalse(html.contains("{{"))
    }

    @Test
    fun `공유 카드는 1200x630 PNG로 그린다`() {
        val png = ShareImageRenderer().render(card)
        val signature = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        assertTrue(png.copyOfRange(0, 4).contentEquals(signature))
    }

    private fun renderPage(): String = SharePageRenderer().render(card, PUBLIC_ORIGIN)

    private fun assertContainsAll(html: String, vararg fragments: String) {
        fragments.forEach { assertTrue(html.contains(it), "공유 페이지에 다음 조각이 없습니다: $it") }
    }

    companion object {
        private const val PUBLIC_ORIGIN = "https://santacut.example"
    }
}
