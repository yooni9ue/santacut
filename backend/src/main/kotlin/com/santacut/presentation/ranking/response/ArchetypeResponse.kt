package com.santacut.presentation.ranking.response

import io.swagger.v3.oas.annotations.media.Schema

data class ArchetypeResponse(
    @field:Schema(
        description = "산타 유형 코드. 일러스트를 고를 때 씁니다.",
        allowableValues = [
            "HOT_COCOA_COURIER", "FIREPLACE_KEEPER", "CAROL_LEADER", "TREE_TOP_STAR", "MANNER_SNOWMAN",
            "GIFT_WRAPPER", "RUDOLPH_DRIVER", "WORKSHOP_ELF", "SANTA_CANDIDATE", "GRINCH_APPRENTICE",
        ],
        example = "HOT_COCOA_COURIER",
    )
    val code: String,
    @field:Schema(description = "산타 유형 한국어 이름", example = "핫초코 배달부")
    val name: String,
)
