package com.santacut.presentation.analysis.request

import com.santacut.application.analysis.command.SubmitAnalysisCommand
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import java.util.UUID

data class SubmitAnalysisRequest(
    @field:Pattern(regexp = SOURCE_PATTERN)
    @field:Schema(description = "내보내기 플랫폼", example = KAKAO_SOURCE, allowableValues = [KAKAO_SOURCE, INSTAGRAM_SOURCE])
    val source: String,
    @field:ArraySchema(
        arraySchema = Schema(
            description = "정규화한 이벤트 20~2,000개. 가능하면 occurredAt 순서로 보냅니다. " +
                "20개보다 적으면 INSUFFICIENT_EVENTS, 2,000개보다 많으면 TOO_MANY_EVENTS입니다.",
        ),
        schema = Schema(implementation = AnalysisEventRequest::class),
        minItems = MIN_EVENTS,
        maxItems = MAX_EVENTS,
    )
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

        /** Swagger 스키마에 적는 이벤트 수의 하한과 상한. 검증은 도메인이 하고 오류 코드로 알린다. */
        const val MIN_EVENTS = 20
        const val MAX_EVENTS = 2000
    }
}
