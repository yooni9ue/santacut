package com.santacut.presentation.prompt.request

import com.santacut.application.prompt.command.CreatePromptCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class CreatePromptRequest(
    @field:Size(min = MIN_CONTENT_LENGTH, max = MAX_CONTENT_LENGTH)
    @field:Schema(
        description = "새 판의 프롬프트 본문. 1~20,000자입니다. 다음 AI 호출부터 이 판을 씁니다.",
        example = "너는 재미 목적으로만 점수를 매기는 SantaCut Scorer다.",
        minLength = MIN_CONTENT_LENGTH,
        maxLength = MAX_CONTENT_LENGTH,
    )
    val content: String,
) {
    fun toCommand(name: String): CreatePromptCommand = CreatePromptCommand(name, content)

    companion object {
        const val MIN_CONTENT_LENGTH = 1
        const val MAX_CONTENT_LENGTH = 20_000
    }
}
