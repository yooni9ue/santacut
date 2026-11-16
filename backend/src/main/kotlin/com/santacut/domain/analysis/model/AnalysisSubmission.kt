package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.SeoulTime
import java.time.Instant

/**
 * 분석 요청 본문. 그대로 JSON으로 바꿔 작업 페이로드로 저장하고, 그 해시로 재전송을 알아본다.
 */
class AnalysisSubmission(
    val source: DataSource,
    val events: AnalysisEvents,
) {
    fun verifyAcceptableAt(now: Instant) = events.verifyAcceptableAt(now)

    /** 채점 입력(활동 통계, 항목별 낱말 신호, 표본)을 한국 시각 기준으로 만든다. 이벤트 원문 전체는 AI에 보내지 않는다. */
    fun toScoringDigest(): ScoringDigest {
        val messages = events.toScoringMessages(SeoulTime.ZONE)
        val sample = messages.sample()
        return ScoringDigest(source, messages.toFeatures(), messages.signals(sample), sample)
    }
}
