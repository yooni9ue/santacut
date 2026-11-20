package com.santacut.presentation.roast.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "팩폭 카드 옆에 찍는 서버 숫자 칩")
data class ChipResponse(
    @field:Schema(description = "칩 이름", example = "ㅋ 한 번에 최대")
    val label: String,
    @field:Schema(description = "칩 값. 서버가 센 숫자입니다.", example = "12개")
    val value: String,
)
