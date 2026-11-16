package com.santacut.presentation.analysis.stream

import com.santacut.application.analysis.result.AnalysisStatusResult
import com.santacut.presentation.analysis.response.AnalysisStatusResponse

/**
 * 진행 상황 스트림으로 보낼 `status` 이벤트 하나.
 *
 * @property jobId 이벤트가 가리키는 분석 작업
 * @property eventId SSE `id`. 진행 단계 순번이다
 * @property terminal 이 이벤트를 보낸 뒤 스트림을 닫는지
 * @property body 이벤트 `data`. 폴링 상태 응답과 같은 JSON이다
 */
data class AnalysisStatusEvent(
    val jobId: String,
    val eventId: Int,
    val terminal: Boolean,
    val body: AnalysisStatusResponse,
) {
    companion object {
        fun from(result: AnalysisStatusResult): AnalysisStatusEvent =
            AnalysisStatusEvent(result.id, result.eventId, result.terminal, AnalysisStatusResponse.from(result))
    }
}
