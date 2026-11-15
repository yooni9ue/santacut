package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.math.BigDecimal

/**
 * AI 모델이 돌려준 항목별 점수와 근거 이벤트 번호. 검증을 통과해야 [Score]가 된다.
 *
 * @property dimensionScores [ScoreDimension] 순서의 점수
 * @property evidenceIndices 항목별 근거 이벤트 번호
 * @property shape 형식 오류 로그에 남길 scores·evidence 노드 종류
 */
class ScoreAnswer private constructor(
    private val readable: Boolean,
    private val sufficient: Boolean,
    private val dimensionScores: DimensionScores,
    private val evidenceIndices: EvidenceIndices,
    private val shape: String,
) {
    fun toScore(eventCount: Int): Score {
        if (!readable) throw SantaCutException(ErrorCode.AI_RESPONSE_INVALID)
        if (!sufficient) throw SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)
        verifySchema()
        verifyEvidence(eventCount)
        return Score.weighted(dimensionScores.toComponents())
    }

    private fun verifySchema() {
        val complete = dimensionScores.isComplete() && evidenceIndices.isComplete()
        if (!complete) throw SantaCutException(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, shape)
    }

    private fun verifyEvidence(eventCount: Int) {
        val invalidIndex = evidenceIndices.firstOutOfRange(eventCount) ?: return
        throw SantaCutException(ErrorCode.AI_EVIDENCE_INVALID, "count=$eventCount,index=$invalidIndex")
    }

    companion object {
        private val NO_SCORES = DimensionScores(emptyList())
        private val NO_EVIDENCE = EvidenceIndices(emptyList())

        /** 응답이 비었거나 너무 길어 읽지 않았다. */
        fun unreadable(): ScoreAnswer = ScoreAnswer(false, false, NO_SCORES, NO_EVIDENCE, "")

        /** 모델이 근거 부족이라고 답했다. */
        fun insufficient(): ScoreAnswer = ScoreAnswer(true, false, NO_SCORES, NO_EVIDENCE, "")

        /**
         * @param dimensionScores [ScoreDimension] 순서의 점수. 숫자가 아니면 null이다.
         * @param evidenceIndices 항목별 1부터 시작하는 근거 이벤트 번호. 해석할 수 없는 값은 -1이다.
         */
        fun of(dimensionScores: List<BigDecimal?>, evidenceIndices: List<List<Int>>, shape: String): ScoreAnswer =
            ScoreAnswer(true, true, DimensionScores(dimensionScores), EvidenceIndices(evidenceIndices), shape)
    }
}
