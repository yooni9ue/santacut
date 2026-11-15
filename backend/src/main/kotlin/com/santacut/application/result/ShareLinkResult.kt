package com.santacut.application.result

import com.santacut.domain.model.ShareLink

/**
 * 공유 토큰과 공개 HTML·PNG 주소.
 */
data class ShareLinkResult(
    val token: String,
    val url: String,
    val imageUrl: String,
) {
    companion object {
        fun from(link: ShareLink): ShareLinkResult = ShareLinkResult(link.token, link.url, link.imageUrl)
    }
}
