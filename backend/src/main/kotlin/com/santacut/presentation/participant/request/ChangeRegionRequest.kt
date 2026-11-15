package com.santacut.presentation.participant.request

import com.santacut.application.participant.command.ChangeRegionCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ChangeRegionRequest(
    @field:Schema(description = "지역 목록 API(GET /v1/regions)가 돌려준 지역 중 selectable이 true인 코드", example = "KR-11-650")
    val regionCode: String,
) {
    fun toCommand(participantId: UUID): ChangeRegionCommand = ChangeRegionCommand(participantId, regionCode)
}
