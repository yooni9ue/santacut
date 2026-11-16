package com.santacut.domain.analysis.model

/**
 * 채점 대화문의 줄 목록. 시각순이고, AI는 1부터 붙인 줄 번호를 근거 번호로 돌려준다.
 */
class TranscriptTurns(private val values: List<TranscriptTurn>) {
    fun size(): Int = values.size

    /** 본인 줄 본문 글자 수의 합. */
    fun selfLength(): Int = values.filter { turn -> turn.isSelf() }.sumOf { turn -> turn.length() }

    /** 모든 줄 본문 글자 수의 합. */
    fun totalLength(): Int = values.sumOf { turn -> turn.length() }

    /** 최근 줄부터 본문 글자 수 합이 [maxChars]를 넘기 전까지만 남긴다. 순서는 시각순 그대로다. */
    fun recentWithin(maxChars: Int): TranscriptTurns {
        val kept = values.asReversed().runningFold(0) { total, turn -> total + turn.length() }
            .drop(1)
            .count { total -> total <= maxChars }
        return TranscriptTurns(values.takeLast(kept))
    }

    /** 1부터 시작하는 번호와 바로 앞 줄(첫 줄이면 null)을 함께 넘겨 각 줄을 바꾼다. */
    fun <T> mapNumbered(transform: (Int, TranscriptTurn, TranscriptTurn?) -> T): List<T> =
        values.mapIndexed { index, turn -> transform(index + FIRST_NUMBER, turn, values.getOrNull(index - 1)) }

    companion object {
        private const val FIRST_NUMBER = 1
    }
}
