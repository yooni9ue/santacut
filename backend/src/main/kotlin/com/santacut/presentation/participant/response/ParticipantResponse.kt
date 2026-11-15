package com.santacut.presentation.participant.response

import com.santacut.application.participant.result.RegisteredParticipantResult
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ParticipantResponse(
    @field:Schema(description = "새 참가자 UUID", format = "uuid", example = "7af6f50a-4a88-4c32-9be5-a210a8ce1234")
    val id: UUID,
    @field:Schema(description = "정규화한 닉네임", example = "산타도와줘")
    val nickname: String,
    @field:Schema(description = "등록한 지역 코드", example = "KR-11-650")
    val regionCode: String,
) {
    companion object {
        fun from(result: RegisteredParticipantResult): ParticipantResponse =
            ParticipantResponse(result.id, result.nickname, result.regionCode)
    }
}
