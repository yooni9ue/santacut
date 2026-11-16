package com.santacut.presentation.prompt.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.prompt.result.PromptVersionResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class PromptVersionResponse(
    @field:Schema(description = "프롬프트 이름", example = "score")
    val name: String,
    @field:Schema(description = "판 번호", example = "3")
    val version: Int,
    @field:JsonProperty("created_at")
    @field:Schema(description = "판을 저장한 시각(UTC)", format = "date-time", example = "2026-11-16T03:00:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(result: PromptVersionResult): PromptVersionResponse =
            PromptVersionResponse(result.name, result.version, result.createdAt)
    }
}
