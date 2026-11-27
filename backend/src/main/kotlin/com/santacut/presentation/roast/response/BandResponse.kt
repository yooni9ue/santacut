package com.santacut.presentation.roast.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "컷 표의 구간 한 칸")
data class BandResponse(
    @field:Schema(description = "구간 코드", allowableValues = ["PREMIUM", "GIFT_DEPARTED", "SCRAPED_THROUGH", "ONE_STEP_SHORT", "OFF_LIST", "COAL"])
    val code: String,
    @field:Schema(description = "구간 이름", example = "굴뚝에 걸쳐서 통과")
    val label: String,
    @field:Schema(description = "점수 구간 표시", example = "65~74")
    val range: String,
    @field:Schema(description = "내 점수가 든 칸이면 true입니다. 이 칸만 불을 켭니다.")
    val current: Boolean,
)
