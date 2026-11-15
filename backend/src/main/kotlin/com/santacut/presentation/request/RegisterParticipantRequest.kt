package com.santacut.presentation.request

import com.santacut.application.command.RegisterParticipantCommand
import io.swagger.v3.oas.annotations.media.Schema

data class RegisterParticipantRequest(
    @field:Schema(
        description = "중복될 수 없는 닉네임. NFKC로 정규화하며 한글, 영문, 숫자, 밑줄로 된 2~16자입니다.",
        example = "산타도와줘",
        minLength = 2,
        maxLength = 16,
    )
    val nickname: String,
    @field:Schema(description = "지역 목록 API(GET /v1/regions)가 돌려준 지역 코드 중 하나", example = "KR-11-650")
    val regionCode: String,
) {
    fun toCommand(): RegisterParticipantCommand = RegisterParticipantCommand(nickname, regionCode)
}
