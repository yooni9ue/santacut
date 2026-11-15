package com.santacut.domain.share.model

/**
 * 공유 토큰과 공개 HTML·PNG 주소.
 */
class ShareLink private constructor(
    val token: String,
    val url: String,
    val imageUrl: String,
) {
    companion object {
        fun of(publicOrigin: String, share: Share): ShareLink {
            val pageUrl = "$publicOrigin/s/${share.token}"
            return ShareLink(share.token, pageUrl, "$pageUrl/image.png")
        }
    }
}
