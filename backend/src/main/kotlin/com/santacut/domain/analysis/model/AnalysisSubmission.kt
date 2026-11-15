package com.santacut.domain.analysis.model

import java.time.Instant

/**
 * 분석 요청 본문. 그대로 JSON으로 바꿔 작업 페이로드로 저장하고, 그 해시로 재전송을 알아본다.
 */
class AnalysisSubmission(
    val source: DataSource,
    val events: AnalysisEvents,
) {
    fun eventCount(): Int = events.size()

    fun verifyAcceptableAt(now: Instant) = events.verifyAcceptableAt(now)
}
