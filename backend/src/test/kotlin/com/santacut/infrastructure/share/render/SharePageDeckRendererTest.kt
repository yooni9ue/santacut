package com.santacut.infrastructure.share.render

import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 리캡 카드 덱이 있는 공유 페이지와 공유 페이지 보안 정책. */
class SharePageDeckRendererTest {
    private val renderer = SharePageRenderer()

    @Test
    fun `카드 덱 페이지는 모든 카드를 저장한 순서대로 장수와 함께 그린다`() {
        val html = renderer.renderDeck(CARD, deckOf(VERDICT, ROAST, DIMENSIONS), PUBLIC_ORIGIN)

        assertTrue(html.contains("공유 당시 리캡 카드 <strong>3장</strong>"))
        val positions = listOf("1 / 3", "2 / 3", "3 / 3").map { html.indexOf("<span class=\"card-index\">$it</span>") }
        assertTrue(positions.all { it >= 0 } && positions == positions.sorted(), "카드 순서: $positions")
        assertTrue(html.indexOf("단톡방 자판기") < html.indexOf("ㅋ 폭격기") && html.indexOf("ㅋ 폭격기") < html.indexOf("올해 고마웠어"))
        assertContainsAll(
            html, "<li><a href=\"#card-1\" data-jump=\"0\">1. 판정</a></li>", "<span class=\"card-score\">73.73점</span>",
            "<span class=\"seal\">겨우 실었다</span>", "<b>ㅋ 최대</b> 12개", "근데 다정함", "<span style=\"width:72%\">",
        )
        assertFalse(html.contains("{{"))
    }

    @Test
    fun `카드 덱 페이지도 요약 페이지와 같은 Open Graph 메타데이터와 미리보기 PNG를 그린다`() {
        val token = CARD.token
        listOf(renderer.render(CARD, PUBLIC_ORIGIN), renderer.renderDeck(CARD, deckOf(VERDICT), PUBLIC_ORIGIN)).forEach { html ->
            assertContainsAll(
                html,
                "<meta property=\"og:title\" content=\"산타도와줘 · 산타컷 82.45점\">",
                "<meta property=\"og:description\" content=\"공동 3위 · 선물 도착 예정 2026년 12월 25일 · 선물 배송 출발\">",
                "<meta property=\"og:url\" content=\"https://santacut.example/s/$token\">",
                "<meta property=\"og:image\" content=\"https://santacut.example/s/$token/image.png\">",
                "<meta property=\"og:image:width\" content=\"1200\">", "<meta property=\"og:image:height\" content=\"630\">",
                "<meta name=\"twitter:card\" content=\"summary_large_image\">", "<img src=\"/s/$token/image.png\"",
            )
        }
    }

    @Test
    fun `카드 글에 든 태그와 따옴표는 이스케이프해 스크립트 태그로 해석되지 않는다`() {
        val malicious = "<script>alert(1)</script>\"'</script>\u2028"
        val html = renderer.renderDeck(
            CARD, deckOf(VERDICT.copy(headline = malicious, rows = ShareSlideRows(listOf(ShareSlideRow(malicious, malicious))))), PUBLIC_ORIGIN,
        )

        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;&quot;&#39;&lt;/script&gt;\u2028"))
        assertFalse(html.contains("<script>alert"))
        assertEquals(1, Regex("<script").findAll(html).count())
        assertEquals(1, Regex("</script>").findAll(html).count())
    }

    @Test
    fun `보안 정책은 넘김 스크립트의 해시와 공개 출처 글꼴만 허용한다`() {
        val html = renderer.renderDeck(CARD, deckOf(VERDICT), PUBLIC_ORIGIN)
        val script = html.substringAfter("<script>").substringBefore("</script>")
        val hash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(script.toByteArray(Charsets.UTF_8)))

        assertEquals(
            "default-src 'none'; script-src 'sha256-$hash'; style-src 'unsafe-inline'; img-src 'self' data:; " +
                "font-src 'self' https://santacut.example; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
            renderer.contentSecurityPolicy(PUBLIC_ORIGIN),
        )
        assertTrue(html.contains("src:url('https://santacut.example/fonts/Gaegu-Bold.ttf')"))
    }

    private fun deckOf(vararg slides: ShareSlide): ShareDeck = ShareDeck(true, ShareSlides(slides.toList()))

    private fun assertContainsAll(html: String, vararg fragments: String) {
        fragments.forEach { assertTrue(html.contains(it), "공유 페이지에 다음 조각이 없습니다: $it") }
    }

    companion object {
        private const val PUBLIC_ORIGIN = "https://santacut.example"
        private val CARD = ShareCard(
            "T".repeat(43), "산타도와줘", "82.45", "공동 3위", "선물 배송 출발", "2026년 12월 25일", "한마디",
            "2026년 12월 1일 10:02", "미리보기", "2026년 12월 2일 12:30",
        )
        private val VERDICT = ShareSlide(
            "VERDICT", "판정", "73.73", "단톡방 자판기", "겨우 실었다", ShareSlideRows(listOf(ShareSlideRow("등급", "굴뚝에 걸쳐서 통과"))),
        )
        private val ROAST = ShareSlide("ROAST", "팩폭", "73.73", "ㅋ 폭격기", rows = ShareSlideRows(listOf(ShareSlideRow("ㅋ 최대", "12개"), ShareSlideRow("", "근데 다정함"))))
        private val DIMENSIONS = ShareSlide(
            "DIMENSIONS", "항목 점수", "73.73", "올해 고마웠어", bars = ShareSlideBars(listOf(ShareSlideBar("공감", "71.67", 72))),
        )
    }
}
