package com.santacut.application.prompt.result

import com.santacut.domain.prompt.model.Prompt
import java.time.Instant

/**
 * 본문을 뺀 프롬프트 판 정보.
 */
data class PromptVersionResult(
    val name: String,
    val version: Int,
    val createdAt: Instant,
) {
    companion object {
        fun from(prompt: Prompt): PromptVersionResult = PromptVersionResult(prompt.name, prompt.version, prompt.createdAt)
    }
}
