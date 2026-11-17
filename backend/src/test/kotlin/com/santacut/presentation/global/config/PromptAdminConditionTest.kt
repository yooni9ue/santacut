package com.santacut.presentation.global.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class PromptAdminConditionTest {
    private val runner = ApplicationContextRunner().withUserConfiguration(PromptAdminConditionTestConfiguration::class.java)

    @Test
    fun `켜고 토큰도 있어야 관리 API가 뜬다`() {
        assertLoaded(true, "santacut.prompt-admin.enabled=true", "santacut.prompt-admin.token=token")
    }

    @Test
    fun `켜도 토큰이 없거나 비어 있으면 뜨지 않는다`() {
        assertLoaded(false, "santacut.prompt-admin.enabled=true")
        assertLoaded(false, "santacut.prompt-admin.enabled=true", "santacut.prompt-admin.token=  ")
    }

    @Test
    fun `토큰이 있어도 끄면 뜨지 않는다`() {
        assertLoaded(false, "santacut.prompt-admin.token=token")
    }

    private fun assertLoaded(expected: Boolean, vararg properties: String) {
        runner.withPropertyValues(*properties).run { context ->
            assertEquals(expected, context.containsBean(PromptAdminConditionTestConfiguration.BEAN_NAME))
        }
    }
}
