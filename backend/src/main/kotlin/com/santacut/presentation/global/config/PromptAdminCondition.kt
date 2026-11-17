package com.santacut.presentation.global.config

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * 프롬프트 관리 API를 띄울지 정한다. `santacut.prompt-admin.enabled=true`이고 토큰(`PROMPT_ADMIN_TOKEN`)도 있어야 한다.
 * 토큰이 없으면 켜 두어도 인증 없이 열리지 않도록 컨트롤러를 올리지 않는다(404).
 */
class PromptAdminCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val environment = context.environment
        val enabled = environment.getProperty(ENABLED_PROPERTY, Boolean::class.java, false)
        return enabled && !environment.getProperty(TOKEN_PROPERTY).isNullOrBlank()
    }

    companion object {
        private const val ENABLED_PROPERTY = "santacut.prompt-admin.enabled"
        private const val TOKEN_PROPERTY = "santacut.prompt-admin.token"
    }
}
