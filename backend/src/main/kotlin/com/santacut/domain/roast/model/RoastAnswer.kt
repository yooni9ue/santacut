package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.ModelUsage

/** 팩폭 작가 호출 한 번의 응답 본문과 사용량. 호출이 실패했거나 본문이 없으면 [text]가 null이다. */
data class RoastAnswer(val text: String?, val usage: ModelUsage)
