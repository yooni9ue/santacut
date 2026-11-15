package com.santacut.presentation.analysis.request

import com.santacut.application.analysis.command.SubmitAnalysisCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class SubmitAnalysisRequest(
    @field:Pattern(regexp = SOURCE_PATTERN)
    @field:Schema(description = "내보내기 플랫폼", example = KAKAO_SOURCE, allowableValues = [KAKAO_SOURCE, INSTAGRAM_SOURCE])
    val source: String,
    @field:Size(min = MIN_EVENTS, max = MAX_EVENTS)
    @field:Schema(description = "정규화한 이벤트 20~2,000개. 가능하면 occurredAt 순서로 보냅니다.")
    val events: List<@Valid AnalysisEventRequest>,
) {
    fun toCommand(participantId: UUID, idempotencyKey: String): SubmitAnalysisCommand =
        SubmitAnalysisCommand(participantId, idempotencyKey, source, AnalysisEventRequests(events).toCommands())

    companion object {
        /** 내보내기 플랫폼 값. 도메인의 출처 열거형 상수 이름과 같아야 한다. */
        const val KAKAO_SOURCE = "KAKAO"
        const val INSTAGRAM_SOURCE = "INSTAGRAM"

        /** [KAKAO_SOURCE]·[INSTAGRAM_SOURCE] 중 하나만 받는 검증 정규식. */
        const val SOURCE_PATTERN = "$KAKAO_SOURCE|$INSTAGRAM_SOURCE"

        /** 한 번에 받는 이벤트 수의 하한과 상한. */
        const val MIN_EVENTS = 20
        const val MAX_EVENTS = 2000
    }
}
