package com.santacut.domain.prompt.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PromptNameTest {
    @Test
    fun `영문 소문자, 숫자, 하이픈으로 된 이름은 그대로 받는다`() {
        assertEquals("score-v2", PromptName.from("score-v2").value)
    }

    @Test
    fun `1자와 40자는 받는다`() {
        assertEquals("a", PromptName.from("a").value)
        assertEquals(MAX_NAME, PromptName.from(MAX_NAME).value)
    }

    @Test
    fun `비었거나 40자를 넘으면 INVALID_REQUEST다`() {
        assertErrorCode(ErrorCode.INVALID_REQUEST) { PromptName.from("") }
        assertErrorCode(ErrorCode.INVALID_REQUEST) { PromptName.from(MAX_NAME + "a") }
    }

    @Test
    fun `대문자, 밑줄, 공백, 한글이 있으면 INVALID_REQUEST다`() {
        assertErrorCode(ErrorCode.INVALID_REQUEST) { PromptName.from("Score") }
        assertErrorCode(ErrorCode.INVALID_REQUEST) { PromptName.from("score_v2") }
        assertErrorCode(ErrorCode.INVALID_REQUEST) { PromptName.from(" score") }
        assertErrorCode(ErrorCode.INVALID_REQUEST) { PromptName.from("채점") }
    }

    @Test
    fun `채점 프롬프트 이름은 score다`() {
        assertEquals("score", PromptName.SCORE.value)
    }

    companion object {
        private val MAX_NAME = "a".repeat(PromptName.MAX_LENGTH)
    }
}
