package com.santacut.infrastructure.client

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.santacut.domain.model.AnalysisSubmission
import com.santacut.domain.model.DataSource

/**
 * AI 모델에 보내는 사용자 메시지 본문. 이벤트마다 1부터 시작하는 번호를 붙인다.
 */
@JsonPropertyOrder("source", "events")
class ScoreModelInput(
    val source: DataSource,
    val events: List<ScoreModelEvent>,
) {
    companion object {
        fun from(submission: AnalysisSubmission): ScoreModelInput = ScoreModelInput(
            submission.source,
            submission.events.mapNumbered { number, event -> ScoreModelEvent(number, event.occurredAt, event.text) },
        )
    }
}
