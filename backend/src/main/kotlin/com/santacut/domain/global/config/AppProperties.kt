package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

/**
 * 도메인 규칙이 읽는 공개 주소, 순위 시즌, 산타 출발 좌표 설정.
 * 같은 `app` 접두사의 세션 쿠키 설정은 웹 계층의 [com.santacut.presentation.global.config.SessionCookieProperties]가 읽는다.
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val publicBaseUrl: String,
    val seasonYear: Int,
    val originLat: Double,
    val originLon: Double,
) {
    /** 끝의 슬래시를 뗀 공개 출처 URL. */
    val publicOrigin: String = publicBaseUrl.trimEnd('/')

    init {
        require(isOriginUrl(URI(publicOrigin))) { "APP_PUBLIC_BASE_URL은 경로, 쿼리, 사용자 정보가 없는 출처 URL이어야 합니다." }
    }

    private fun isOriginUrl(uri: URI): Boolean =
        uri.scheme in ALLOWED_SCHEMES && uri.host != null && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty()

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}
