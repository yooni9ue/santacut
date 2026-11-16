package com.santacut.domain.analysis.model

/**
 * 채점 구간 하나와 그 구간에 대한 AI 답.
 */
class ScoringChunkAnswer(
    val chunk: ScoringChunk,
    val answer: ScoreAnswer,
) {
    /** 근거 번호를 이 구간 줄 범위로 거른 항목별 점수. */
    fun readings(): DimensionReadings = answer.readings(chunk.lineRange(), chunk.turnWeight())

    /** 장난기 × 구간 본인 턴 무게. 장난기를 돌려주지 않았으면 null이다. */
    fun weightedPlayfulness(): Int? = answer.playfulness?.let { playfulness -> playfulness * chunk.turnWeight() }

    /** 범위 밖·겹친 근거 번호 수. */
    fun discardedEvidence(): Int = answer.discardedEvidence(chunk.lineRange())

    /** [dimension]의 근거 줄 가운데 [maxChars]자 이하 본인 토막 하나. 없으면 null이다. */
    fun quoteFor(dimension: ScoreDimension, maxChars: Int): String? =
        readings().quoteOf(dimension) { number -> chunk.selfQuoteAt(number, maxChars) }
}
