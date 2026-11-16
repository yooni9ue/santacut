package com.santacut.application.prompt.result

import com.santacut.domain.prompt.model.Prompt
import java.time.Instant

/**
 * 본문을 포함한 프롬프트 한 판.
 */
data class PromptResult(
    val name: String,
    val version: Int,
    val content: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(prompt: Prompt): PromptResult = PromptResult(prompt.name, prompt.version, prompt.content, prompt.createdAt)
    }
}
