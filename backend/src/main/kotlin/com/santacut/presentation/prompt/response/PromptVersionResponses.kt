package com.santacut.presentation.prompt.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.prompt.result.PromptVersionResults

/**
 * 프롬프트 판 목록 응답. JSON에서는 높은 판부터 [PromptVersionResponse] 배열 하나로 나간다.
 */
class PromptVersionResponses(@field:JsonValue private val values: List<PromptVersionResponse>) {
    companion object {
        fun from(results: PromptVersionResults): PromptVersionResponses =
            PromptVersionResponses(results.map(PromptVersionResponse::from))
    }
}
