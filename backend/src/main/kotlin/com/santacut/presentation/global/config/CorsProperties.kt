package com.santacut.presentation.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 다른 출처의 프론트엔드가 쿠키를 실어 API를 부를 수 있게 허용할 출처 목록.
 *
 * 자격 증명(쿠키)을 허용하므로 `*` 같은 와일드카드는 받지 않는다.
 *
 * @property allowedOrigins 허용할 출처(스킴·호스트·포트) 목록
 */
@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: List<String>,
) {
    init {
        require(allowedOrigins.none { it.contains(WILDCARD) }) { "APP_CORS_ALLOWED_ORIGINS에는 와일드카드(*)를 쓸 수 없습니다." }
    }

    companion object {
        private const val WILDCARD = "*"
    }
}
