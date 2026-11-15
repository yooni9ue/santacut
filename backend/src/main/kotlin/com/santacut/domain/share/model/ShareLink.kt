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
        /** 공개 공유 페이지 경로 접두어. 뒤에 토큰이 붙는다. */
        private const val PAGE_PATH_PREFIX = "/s/"

        /** 공유 페이지 주소 뒤에 붙는 카드 이미지 경로. */
        private const val IMAGE_PATH_SUFFIX = "/image.png"

        fun of(publicOrigin: String, share: Share): ShareLink {
            val pageUrl = "$publicOrigin$PAGE_PATH_PREFIX${share.token}"
            return ShareLink(share.token, pageUrl, "$pageUrl$IMAGE_PATH_SUFFIX")
        }
    }
}
