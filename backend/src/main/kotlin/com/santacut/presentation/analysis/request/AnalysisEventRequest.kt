package com.santacut.presentation.analysis.request

import com.santacut.application.analysis.command.AnalysisEventCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.Instant

data class AnalysisEventRequest(
    @field:Schema(
        description = "이벤트 시각(ISO-8601 UTC)",
        type = "string",
        format = "date-time",
        example = "2026-12-01T08:30:00Z",
    )
    val occurredAt: Instant,
    @field:Size(min = MIN_TEXT_LENGTH, max = MAX_TEXT_LENGTH)
    @field:Schema(
        description = "메시지 본문이나 정규화한 활동 설명. 1~2,000자입니다.",
        example = "친구가 힘들어해서 먼저 연락하고 이야기를 들어줬어.",
        minLength = MIN_TEXT_LENGTH,
        maxLength = MAX_TEXT_LENGTH,
    )
    val text: String,
) {
    fun toCommand(): AnalysisEventCommand = AnalysisEventCommand(occurredAt, text)

    companion object {
        /** 이벤트 본문 길이의 하한과 상한(글자 수). 검증과 Swagger 스키마가 함께 쓴다. */
        const val MIN_TEXT_LENGTH = 1
        const val MAX_TEXT_LENGTH = 2000
    }
}
