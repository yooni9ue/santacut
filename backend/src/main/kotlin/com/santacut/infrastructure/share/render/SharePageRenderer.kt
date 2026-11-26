package com.santacut.infrastructure.share.render

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64

/**
 * 공유 결과를 Open Graph 메타데이터가 있는 HTML 페이지로 그린다.
 * 페이지 틀은 `share/` 아래 템플릿에 있고, `{{이름}}` 자리표시자를 한 번에 바꿔 넣는다.
 * 리캡 카드 덱이 있는 링크는 카드 덱 페이지로, 없는 예전 링크는 요약 페이지로 그린다.
 */
@Component
class SharePageRenderer {
    private val template: String by lazy { load(TEMPLATE_PATH) }
    private val deckTemplate: String by lazy { load(DECK_TEMPLATE_PATH) }
    private val inactiveTemplate: String by lazy { load(INACTIVE_TEMPLATE_PATH) }
    private val santa: String by lazy { load(SANTA_PATH) }
    private val deckScript: String by lazy { load(DECK_SCRIPT_PATH) }

    /** 카드 덱 페이지의 인라인 스크립트 해시. 스크립트는 고정 리소스라 해시도 한 번만 센다. */
    private val deckScriptHash: String by lazy {
        Base64.getEncoder().encodeToString(MessageDigest.getInstance(HASH_ALGORITHM).digest(deckScript.toByteArray(Charsets.UTF_8)))
    }

    fun render(card: ShareCard, publicOrigin: String): String = fill(template, SharePageView.of(card, publicOrigin))

    /** 요약에 더해 공유 당시 리캡 카드 [deck]을 모두 그린다. 스크립트 없이도 모든 카드가 읽힌다. */
    fun renderDeck(card: ShareCard, deck: ShareDeck, publicOrigin: String): String =
        fill(deckTemplate, SharePageView.deck(card, deck, publicOrigin, santa, deckScript))

    /** 공유 링크가 없거나 취소됐을 때 보여 줄 페이지. 서비스 시작 링크만 담는다. */
    fun renderInactive(publicOrigin: String): String = fill(inactiveTemplate, SharePageView.inactive(publicOrigin))

    /**
     * 공유 페이지 응답의 Content-Security-Policy. 인라인 스크립트는 카드 덱 넘김 스크립트의 해시로만 허용하고,
     * 글꼴은 같은 출처와 공개 출처 [publicOrigin]에서, 이미지는 같은 출처(`/s/{token}/image.png`)에서만 읽는다.
     */
    fun contentSecurityPolicy(publicOrigin: String): String = listOf(
        "default-src 'none'",
        "script-src 'sha256-$deckScriptHash'",
        "style-src 'unsafe-inline'",
        "img-src 'self' data:",
        "font-src 'self' $publicOrigin",
        "base-uri 'none'",
        "form-action 'none'",
        "frame-ancestors 'none'",
    ).joinToString(POLICY_SEPARATOR)

    private fun fill(page: String, view: SharePageView): String =
        PLACEHOLDER.replace(page) { view.valueOf(it.groupValues[1]) }

    private fun load(path: String): String =
        ClassPathResource(path).inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trimEnd()

    companion object {
        private const val TEMPLATE_PATH = "share/share-page.html"
        private const val DECK_TEMPLATE_PATH = "share/share-deck.html"
        private const val INACTIVE_TEMPLATE_PATH = "share/share-inactive.html"
        private const val SANTA_PATH = "share/santa.svg"
        private const val DECK_SCRIPT_PATH = "share/share-deck.js"
        private const val HASH_ALGORITHM = "SHA-256"
        private const val POLICY_SEPARATOR = "; "
        private val PLACEHOLDER = Regex("\\{\\{(\\w+)}}")
    }
}
