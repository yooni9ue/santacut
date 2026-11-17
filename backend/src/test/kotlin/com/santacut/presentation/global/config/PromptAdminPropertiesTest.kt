package com.santacut.presentation.global.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptAdminPropertiesTest {
    @Test
    fun `설정한 토큰과 같은 값만 받는다`() {
        val properties = PromptAdminProperties(enabled = true, token = TOKEN)

        assertTrue(properties.accepts(TOKEN))
        assertFalse(properties.accepts("wrong"))
        assertFalse(properties.accepts(""))
        assertFalse(properties.accepts(null))
    }

    @Test
    fun `토큰을 설정하지 않았으면 빈 값을 보내도 받지 않는다`() {
        val properties = PromptAdminProperties(enabled = true, token = "")

        assertFalse(properties.accepts(""))
        assertFalse(properties.accepts(null))
    }

    @Test
    fun `문자열로 바꿔도 토큰이 드러나지 않는다`() {
        assertFalse(PromptAdminProperties(enabled = true, token = TOKEN).toString().contains(TOKEN))
    }

    companion object {
        private const val TOKEN = "secret-admin-token"
    }
}
