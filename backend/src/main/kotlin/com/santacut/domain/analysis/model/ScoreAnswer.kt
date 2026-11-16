package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.math.BigDecimal

/**
 * AI 모델이 채점 구간 하나에 돌려준 항목별 점수, 기회 수, 근거 줄 번호와 장난기.
 * 구간 답을 합쳐 점수를 만드는 일은 [ScoringChunkAnswers]가 한다.
 *
 * @property usage 이 호출의 토큰 사용량. 호출하지 못했으면 [ModelUsage.NONE]이다.
 * @property playfulness 순위에 쓰지 않는 장난기 성향(0~100). 돌려주지 않았으면 null이다.
 * @property dimensionScores [ScoreDimension] 순서의 점수
 * @property evidenceIndices 항목별 근거 줄 번호
 * @property opportunities 항목별 기회 수
 * @property shape 형식 오류 로그에 남길 노드 종류
 */
class ScoreAnswer private constructor(
    val usage: ModelUsage,
    val playfulness: Int?,
    private val reply: Reply,
    private val dimensionScores: DimensionScores,
    private val evidenceIndices: EvidenceIndices,
    private val opportunities: DimensionOpportunities,
    private val shape: String,
) {
    /** 점수를 담은 답인지. 근거 부족 답은 다른 구간과 합칠 때 뺀다. */
    fun isScored(): Boolean = reply == Reply.SCORED

    /** 다른 구간을 더 물어도 되는 답인지. 제공자 차단과 읽기 실패면 작업 전체가 그 이유로 끝난다. */
    fun isUsable(): Boolean = reply == Reply.SCORED || reply == Reply.INSUFFICIENT

    /** 제공자 차단, 읽기 실패, 형식 오류, 범위 밖 점수를 던진다. 근거 부족 답은 던지지 않는다. */
    fun verifyAnswered() {
        reply.verify()
        if (!isScored()) return
        if (!dimensionScores.isComplete() || !evidenceIndices.isComplete()) {
            throw SantaCutException(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, shape)
        }
        if (!dimensionScores.isWithinRange()) throw SantaCutException(ErrorCode.AI_SCORE_OUT_OF_RANGE)
    }

    /** 근거 번호를 [lines] 범위로 거른 항목별 점수. [turnWeight]는 구간의 본인 턴 무게다. [verifyAnswered]를 통과한 답에서 부른다. */
    fun readings(lines: IntRange, turnWeight: Int): DimensionReadings =
        if (isScored()) dimensionScores.toReadings(evidenceIndices.cleaned(lines), opportunities, turnWeight) else DimensionReadings.EMPTY

    /** [lines] 범위 밖이거나 겹쳐 지운 근거 번호 수. 사용량 로그에 남긴다. */
    fun discardedEvidence(lines: IntRange): Int = evidenceIndices.discardedCount(lines)

    /** 모델 답의 종류. 근거 부족은 구간 하나의 답으로는 실패가 아니다. */
    private enum class Reply(private val failure: ErrorCode?) {
        SCORED(null),
        UNREADABLE(ErrorCode.AI_RESPONSE_INVALID),
        INSUFFICIENT(null),
        PROVIDER_UNAVAILABLE(ErrorCode.AI_PROVIDER_UNAVAILABLE),
        ;

        fun verify() {
            if (failure != null) throw SantaCutException(failure)
        }
    }

    companion object {
        private val NO_SCORES = DimensionScores(emptyList())
        private val NO_EVIDENCE = EvidenceIndices(emptyList())
        private const val MIN_PLAYFULNESS = 0
        private const val MAX_PLAYFULNESS = 100

        /** 응답이 비었거나 너무 길어 읽지 않았다. */
        fun unreadable(usage: ModelUsage = ModelUsage.NONE): ScoreAnswer = failed(usage, Reply.UNREADABLE)

        /** 모델이 근거 부족이라고 답했다. */
        fun insufficient(usage: ModelUsage = ModelUsage.NONE): ScoreAnswer = failed(usage, Reply.INSUFFICIENT)

        /** 제공자가 호출을 막았거나(권한, 요청 한도) 잠시 쓸 수 없다. 작업을 실패시키지 않고 나중에 다시 잡는다. */
        fun providerUnavailable(): ScoreAnswer = failed(ModelUsage.NONE, Reply.PROVIDER_UNAVAILABLE)

        private fun failed(usage: ModelUsage, reply: Reply): ScoreAnswer =
            ScoreAnswer(usage, null, reply, NO_SCORES, NO_EVIDENCE, DimensionOpportunities.MISSING, "")

        /**
         * @param dimensionScores [ScoreDimension] 순서의 점수. 숫자가 아니면 null이다.
         * @param evidenceIndices 항목별 1부터 시작하는 근거 줄 번호. 해석할 수 없는 값은 -1이다.
         * @param opportunities 항목별 기회 수. 받지 못했으면 null이다.
         * @param playfulness 장난기. 0~100 밖이면 받지 않은 것으로 본다.
         */
        fun of(
            dimensionScores: List<BigDecimal?>,
            evidenceIndices: List<List<Int>>,
            shape: String,
            usage: ModelUsage = ModelUsage.NONE,
            opportunities: List<Int?> = emptyList(),
            playfulness: Int? = null,
        ): ScoreAnswer = ScoreAnswer(
            usage, playfulness?.takeIf { it in MIN_PLAYFULNESS..MAX_PLAYFULNESS }, Reply.SCORED, DimensionScores(dimensionScores), EvidenceIndices(evidenceIndices),
            DimensionOpportunities(opportunities), shape,
        )
    }
}
