package com.santacut.presentation.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.security.MessageDigest

/**
 * dev 전용 프롬프트 관리 API 설정.
 *
 * @property enabled 참이고 [token]도 있을 때만 관리 API가 뜬다
 * @property token 관리 API 요청이 `X-Admin-Token` 헤더로 보내야 하는 값. 비어 있으면 관리 API는 꺼진 채(404)다
 */
@ConfigurationProperties(prefix = "santacut.prompt-admin")
data class PromptAdminProperties(
    val enabled: Boolean = false,
    val token: String = "",
) {
    /** 설정한 토큰이 없거나 [presented]가 그 토큰과 다르면 거짓이다. 길이 말고는 시간 차이가 나지 않게 비교한다. */
    fun accepts(presented: String?): Boolean {
        if (token.isBlank() || presented == null) return false
        return MessageDigest.isEqual(token.toByteArray(), presented.toByteArray())
    }

    override fun toString(): String = "PromptAdminProperties(enabled=$enabled, token=****)"
}
