package com.santacut.presentation.response

import com.santacut.application.result.CurrentParticipantResult
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CurrentParticipantResponse(
    @field:Schema(description = "로그인한 참가자 UUID", format = "uuid")
    val id: UUID,
    @field:Schema(description = "등록한 닉네임")
    val nickname: String,
    @field:Schema(description = "등록한 지역 코드", example = "KR-11-650")
    val regionCode: String,
) {
    companion object {
        fun from(result: CurrentParticipantResult): CurrentParticipantResponse =
            CurrentParticipantResponse(result.id, result.nickname, result.regionCode)
    }
}
