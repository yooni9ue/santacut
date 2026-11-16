package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ScoringDigest

/**
 * AI 모델에 보내는 사용자 메시지 본문. 이벤트마다 JSON 객체를 보내지 않고, 항목별 낱말 신호와 표본 줄만 짧은 글로 보낸다.
 *
 * ```text
 * [signals]
 * care_empathy hit=41 neg=3 weeks=9/13 shown=12
 * ...
 * [sample]
 * # 11-09주
 * 17|내용
 * (3일 뒤)
 * 18|내용
 * ```
 */
class ScoreModelInput(
    private val signalLines: ScoreModelSignalLines,
    private val events: ScoreModelEvents,
) {
    fun toText(): String = SIGNALS_HEADER + signalLines.toText() + SAMPLE_HEADER + events.toText()

    companion object {
        private const val SIGNALS_HEADER = "[signals]\n"
        private const val SAMPLE_HEADER = "\n[sample]\n"

        fun from(digest: ScoringDigest): ScoreModelInput =
            ScoreModelInput(ScoreModelSignalLines.from(digest.signals), ScoreModelEvents.from(digest.sample))
    }
}
