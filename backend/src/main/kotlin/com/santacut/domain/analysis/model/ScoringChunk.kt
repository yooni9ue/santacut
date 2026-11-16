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
    /** 이 구간에 실린 줄 번호 범위. 근거 번호는 이 안에 있어야 한다. */
    fun lineRange(): IntRange = firstNumber until firstNumber + turns.size()

    /** 겹친 앞 줄을 뺀 본인 턴 수. 구간끼리 합칠 때 무게로 쓴다. */
    fun selfTurnCount(): Int = turns.selfCountFrom(overlapTurns)

    /** 줄 번호 [number]가 본인 줄이면 그 안에서 [maxChars]자 이하인 한 토막. 상대 줄이거나 맞는 토막이 없으면 null이다. */
    fun selfQuoteAt(number: Int, maxChars: Int): String? = turns.selfQuoteAt(number - firstNumber, maxChars)

    companion object {
        const val FIRST_NUMBER = 1

        /** 대화문 전체를 한 구간으로 본다. 채점 입력 해시에 쓴다. */
        fun whole(digest: ScoringDigest): ScoringChunk = ScoringChunk(digest.turns, FIRST_NUMBER, 0, digest.omittedTurns)
    }
}
