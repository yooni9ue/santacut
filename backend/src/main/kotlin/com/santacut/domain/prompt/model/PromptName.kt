package com.santacut.domain.prompt.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 프롬프트 이름. 영문 소문자, 숫자, 하이픈으로 된 1~40자다.
 * 규칙을 어기면 [ErrorCode.INVALID_REQUEST]다.
 */
class PromptName private constructor(val value: String) {
    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 40
        private val PATTERN = Regex("[a-z0-9-]{$MIN_LENGTH,$MAX_LENGTH}")

        /** 분석 작업자가 AI 채점에 쓰는 프롬프트 이름. 1판은 prompts.sql이 넣는다. */
        val SCORE: PromptName = from("score")

        fun from(raw: String): PromptName {
            if (!PATTERN.matches(raw)) throw SantaCutException(ErrorCode.INVALID_REQUEST)
            return PromptName(raw)
        }
    }
}
