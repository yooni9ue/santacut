package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * AI 채점에 쓰는 입력. 본인 메시지 전체 대신 항목별 낱말 [signals]와 작은 [sample]만 AI에 보낸다.
 *
 * @property messageCount 본인 메시지 수. AI에는 보내지 않고 사용량 로그에만 남긴다.
 */
class ScoringDigest(
    val messageCount: Int,
    val signals: DimensionSignals,
    val sample: SampleMessages,
) {
    /** AI가 돌려줄 수 있는 근거 번호의 상한. */
    fun sampleSize(): Int = sample.size()

    /**
     * 표본이 [MIN_SAMPLE_LINES]줄보다 적거나 본문이 [MIN_SAMPLE_CHARS]자보다 짧으면 AI를 부르지 않고 근거 부족으로 끝낸다.
     * 이만큼도 없으면 네 항목 모두에 근거를 찾을 가능성이 낮아 호출 비용만 든다. 문턱은 일부러 낮게 잡았다.
     */
    fun verifySampled(): ScoringDigest {
        if (sample.size() < MIN_SAMPLE_LINES || sample.totalLength() < MIN_SAMPLE_CHARS) {
            throw SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)
        }
        return this
    }

    companion object {
        const val MIN_SAMPLE_LINES = 8
        const val MIN_SAMPLE_CHARS = 300
    }
}
