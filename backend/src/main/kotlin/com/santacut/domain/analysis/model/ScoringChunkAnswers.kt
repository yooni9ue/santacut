package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 채점 구간마다 받은 AI 답 목록. 구간 점수를 코드로 합쳐 하나의 [Score]를 만든다.
 *
 * - 제공자 차단·읽기 실패·형식 오류가 한 구간이라도 있으면 작업 전체가 그 이유로 끝난다(원자적).
 * - 근거 부족이라고 답한 구간은 빼고 합친다. 모든 구간이 근거 부족이면 [ErrorCode.INSUFFICIENT_EVIDENCE]다.
 * - 장난기(playfulness)는 순위에 쓰지 않는 성향 축이라 본인 턴 수로 평균만 낸다.
 */
class ScoringChunkAnswers(private val values: List<ScoringChunkAnswer>) {
    /** 모든 구간 호출의 토큰 사용량 합. */
    fun usage(): ModelUsage = values.fold(ModelUsage.NONE) { total, chunkAnswer -> total + chunkAnswer.answer.usage }

    /** 모든 구간에서 범위 밖·겹친 근거 번호 수의 합. */
    fun discardedEvidence(): Int = values.sumOf { chunkAnswer -> chunkAnswer.discardedEvidence() }

    fun size(): Int = values.size

    /** 구간 점수를 항목마다 합치고 [shrinkage]로 수축해 합계를 한 번만 반올림한다. */
    fun toScore(shrinkage: ScoreShrinkage): Score {
        values.forEach { chunkAnswer -> chunkAnswer.answer.verifyAnswered() }
        val scored = values.filter { chunkAnswer -> chunkAnswer.answer.isScored() }
        if (scored.isEmpty()) throw SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)
        val readings = scored.fold(DimensionReadings.EMPTY) { total, chunkAnswer -> total + chunkAnswer.readings() }
        val lowConfidence = readings.lowConfidence()
        verifyAnyEvidence(lowConfidence)
        val selfTurns = scored.sumOf { chunkAnswer -> chunkAnswer.chunk.selfTurnCount() }
        return Score.shrunk(readings.shrunkScores(shrinkage, selfTurns), lowConfidence, playfulnessOf(scored))
    }

    /**
     * [score]에서 점수가 높은 항목부터 근거 줄을 보며 [MAX_QUOTE_CHARS]자 이하 본인 한마디를 고른다.
     * 상대 줄은 고르지 않는다. 없으면 null이다.
     */
    fun quoteOf(score: Score): String? {
        val scored = values.filter { chunkAnswer -> chunkAnswer.answer.isScored() }
        return score.components.rankedDimensions().firstNotNullOfOrNull { dimension ->
            scored.firstNotNullOfOrNull { chunkAnswer -> chunkAnswer.quoteFor(dimension, MAX_QUOTE_CHARS) }
        }
    }

    /** 모든 항목에 근거가 없을 때, 지운 번호가 있었으면 응답 오류, 처음부터 없었으면 근거 부족이다. */
    private fun verifyAnyEvidence(lowConfidence: LowConfidenceDimensions) {
        if (!lowConfidence.coversAll()) return
        val discarded = discardedEvidence()
        if (discarded == 0) throw SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)
        throw SantaCutException(ErrorCode.AI_EVIDENCE_INVALID, "chunks=${values.size},discarded=$discarded")
    }

    /** 장난기를 돌려준 구간의 본인 턴 수 가중 평균(정수). 하나도 없으면 null이다. */
    private fun playfulnessOf(scored: List<ScoringChunkAnswer>): Int? {
        val rated = scored.filter { chunkAnswer -> chunkAnswer.answer.playfulness != null }
        val weight = rated.sumOf { chunkAnswer -> chunkAnswer.chunk.selfTurnCount().coerceAtLeast(1) }
        if (rated.isEmpty()) return null
        val sum = rated.sumOf { chunkAnswer ->
            (chunkAnswer.answer.playfulness ?: 0) * chunkAnswer.chunk.selfTurnCount().coerceAtLeast(1)
        }
        return BigDecimal(sum).divide(BigDecimal(weight), 0, RoundingMode.HALF_UP).toInt()
    }

    companion object {
        /** 결과에 보여 줄 본인 한마디의 최대 글자 수. */
        const val MAX_QUOTE_CHARS = 20
    }
}
