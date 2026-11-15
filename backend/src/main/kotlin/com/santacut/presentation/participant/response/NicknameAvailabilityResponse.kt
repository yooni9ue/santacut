package com.santacut.presentation.participant.response

import com.santacut.application.participant.result.NicknameAvailabilityResult
import io.swagger.v3.oas.annotations.media.Schema

data class NicknameAvailabilityResponse(
    @field:Schema(description = "지금 아무도 쓰지 않는 닉네임인지. 예약이 아니므로 등록할 때 NICKNAME_TAKEN이 날 수 있습니다.", example = "true")
    val available: Boolean,
) {
    companion object {
        fun from(result: NicknameAvailabilityResult): NicknameAvailabilityResponse =
            NicknameAvailabilityResponse(result.available)
    }
}
