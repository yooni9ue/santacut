package com.santacut.infrastructure.analysis.client

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.Instant

/**
 * AI 모델에 보내는 이벤트. 모델은 [index]를 근거 번호로 그대로 돌려준다.
 */
@JsonPropertyOrder("index", "occurredAt", "text")
class ScoreModelEvent(
    val index: Int,
    val occurredAt: Instant,
    val text: String,
)
