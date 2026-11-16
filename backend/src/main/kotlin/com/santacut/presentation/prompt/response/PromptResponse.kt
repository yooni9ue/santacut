package com.santacut.presentation.prompt.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.prompt.result.PromptResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class PromptResponse(
    @field:Schema(description = "프롬프트 이름", example = "score")
    val name: String,
    @field:Schema(description = "판 번호. 1부터 1씩 늘고 가장 높은 판을 AI 호출에 씁니다.", example = "3")
    val version: Int,
    @field:Schema(description = "프롬프트 본문", example = "너는 재미 목적으로만 점수를 매기는 SantaCut Scorer다.")
    val content: String,
    @field:JsonProperty("created_at")
    @field:Schema(description = "판을 저장한 시각(UTC)", format = "date-time", example = "2026-11-16T03:00:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(result: PromptResult): PromptResponse =
            PromptResponse(result.name, result.version, result.content, result.createdAt)
    }
}
