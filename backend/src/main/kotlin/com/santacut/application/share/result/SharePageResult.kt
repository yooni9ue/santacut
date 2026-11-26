package com.santacut.application.share.result

/**
 * 서버에서 그린 공개 공유 페이지 HTML과 그 응답에 붙일 Content-Security-Policy.
 */
data class SharePageResult(
    val html: String,
    val contentSecurityPolicy: String,
)
