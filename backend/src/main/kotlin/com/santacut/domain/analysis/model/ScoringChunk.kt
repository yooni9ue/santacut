package com.santacut.domain.analysis.model

/**
 * 대화문을 나눈 채점 구간 하나. 줄 번호는 대화문 전체 기준([firstNumber]부터)이라 근거 번호가 구간끼리 겹치지 않는다.
 * 앞 구간과 이어지는 답장 맥락을 위해 앞 구간의 마지막 [overlapTurns]줄을 머리에 함께 싣는다.
 *
 * @property omittedTurns 길이 한도로 뺀 앞선 줄 수. 첫 구간만 가진다.
 */
class ScoringChunk(
    val turns: TranscriptTurns,
    val firstNumber: Int,
    private val overlapTurns: Int,
    val omittedTurns: Int,
) {
    /**
     * 이 구간이 근거로 쓸 수 있는 줄 번호 범위. 겹친 앞 줄은 앞 구간 몫이라 뺀다.
     * 그래야 같은 줄이 두 구간의 근거로 두 번 세어지지 않는다.
     */
    fun lineRange(): IntRange = firstNumber + overlapTurns until firstNumber + turns.size()

    /** 앞 구간과 겹친 마지막 줄 번호. 겹친 줄이 없으면 null이다. 모델에 그 줄까지는 맥락뿐이라고 알린다. */
    fun lastOverlapNumber(): Int? = if (overlapTurns == 0) null else lineRange().first - 1

    /** 겹친 앞 줄을 뺀 본인 턴 수. 수축의 근거 무게에 쓴다. */
    fun selfTurnCount(): Int = turns.selfCountFrom(overlapTurns)

    /** 구간끼리 합칠 때의 본인 턴 무게. 본인 턴이 없는 구간도 [MIN_TURN_WEIGHT]만큼은 센다. */
    fun turnWeight(): Int = selfTurnCount().coerceAtLeast(MIN_TURN_WEIGHT)

    /** 줄 번호 [number]가 본인 줄이면 그 안에서 [maxChars]자 이하인 한 토막. 상대 줄이거나 맞는 토막이 없으면 null이다. */
    fun selfQuoteAt(number: Int, maxChars: Int): String? = turns.selfQuoteAt(number - firstNumber, maxChars)

    companion object {
        /** 구간 하나가 합산에서 갖는 가장 작은 본인 턴 무게. */
        const val MIN_TURN_WEIGHT = 1

        /** 대화문 전체를 한 구간으로 본다. 채점 입력 해시에 쓴다. */
        fun whole(digest: ScoringDigest): ScoringChunk = ScoringChunk(digest.turns, TranscriptTurns.FIRST_NUMBER, 0, digest.omittedTurns)
    }
}
