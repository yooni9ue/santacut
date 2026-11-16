package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.math.BigDecimal

/**
 * AI 모델이 돌려준 항목별 점수와 근거 표본 번호. 검증을 통과해야 [Score]가 된다.
 *
 * @property usage 이 호출의 토큰 사용량. 호출하지 못했으면 [ModelUsage.NONE]이다.
 * @property dimensionScores [ScoreDimension] 순서의 점수
 * @property evidenceIndices 항목별 근거 표본 번호
 * @property shape 형식 오류 로그에 남길 scores·evidence 노드 종류
 */
class ScoreAnswer private constructor(
    val usage: ModelUsage,
    private val reply: Reply,
    private val dimensionScores: DimensionScores,
    private val evidenceIndices: EvidenceIndices,
    private val shape: String,
) {
    /** 범위 밖·겹친 근거 번호를 지운 뒤에도 모든 항목에 근거가 남아야 점수가 된다. */
    fun toScore(sampleSize: Int): Score {
        reply.verify()
        verifySchema()
        verifyEvidence(sampleSize)
        return Score.weighted(dimensionScores.toComponents())
    }

    /** 검증할 때 지운 근거 번호 수. 사용량 로그에 남긴다. */
    fun discardedEvidence(sampleSize: Int): Int = evidenceIndices.discardedCount(sampleSize)

    private fun verifySchema() {
        val complete = dimensionScores.isComplete() && evidenceIndices.isComplete()
        if (!complete) throw SantaCutException(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, shape)
    }

    private fun verifyEvidence(sampleSize: Int) {
        if (evidenceIndices.cleaned(sampleSize).isComplete()) return
        val discarded = evidenceIndices.discardedCount(sampleSize)
        throw SantaCutException(ErrorCode.AI_EVIDENCE_INVALID, "count=$sampleSize,discarded=$discarded")
    }

    /** 모델 답의 종류. 점수를 담은 답([SCORED])만 다음 검증으로 넘어간다. */
    private enum class Reply(private val failure: ErrorCode?) {
        SCORED(null),
        UNREADABLE(ErrorCode.AI_RESPONSE_INVALID),
        INSUFFICIENT(ErrorCode.INSUFFICIENT_EVIDENCE),
        PROVIDER_UNAVAILABLE(ErrorCode.AI_PROVIDER_UNAVAILABLE),
        ;

        fun verify() {
            if (failure != null) throw SantaCutException(failure)
        }
    }

    companion object {
        private val NO_SCORES = DimensionScores(emptyList())
        private val NO_EVIDENCE = EvidenceIndices(emptyList())

        /** 응답이 비었거나 너무 길어 읽지 않았다. */
        fun unreadable(usage: ModelUsage = ModelUsage.NONE): ScoreAnswer =
            ScoreAnswer(usage, Reply.UNREADABLE, NO_SCORES, NO_EVIDENCE, "")

        /** 모델이 근거 부족이라고 답했다. */
        fun insufficient(usage: ModelUsage = ModelUsage.NONE): ScoreAnswer =
            ScoreAnswer(usage, Reply.INSUFFICIENT, NO_SCORES, NO_EVIDENCE, "")

        /** 제공자가 호출을 막았거나(권한, 요청 한도) 잠시 쓸 수 없다. 작업을 실패시키지 않고 나중에 다시 잡는다. */
        fun providerUnavailable(): ScoreAnswer =
            ScoreAnswer(ModelUsage.NONE, Reply.PROVIDER_UNAVAILABLE, NO_SCORES, NO_EVIDENCE, "")

        /**
         * @param dimensionScores [ScoreDimension] 순서의 점수. 숫자가 아니면 null이다.
         * @param evidenceIndices 항목별 1부터 시작하는 근거 표본 번호. 해석할 수 없는 값은 -1이다.
         */
        fun of(
            dimensionScores: List<BigDecimal?>,
            evidenceIndices: List<List<Int>>,
            shape: String,
            usage: ModelUsage = ModelUsage.NONE,
        ): ScoreAnswer =
            ScoreAnswer(usage, Reply.SCORED, DimensionScores(dimensionScores), EvidenceIndices(evidenceIndices), shape)
    }
}
