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

    /**
     * 본문 글자 수 합이 [maxChars]를 넘으면 [SpreadCandidates] 규칙으로 달마다 고르게 남긴다. 순서는 시각순 그대로다.
     * 최신 줄만 남기면 대화가 많은 사람은 최근 몇 주만 채점되기 때문이다.
     */
    fun spreadWithin(maxChars: Int): TranscriptTurns =
        TranscriptTurns(SpreadCandidates(values.map { turn -> turn.toSpreadCandidate() }).within(maxChars))

    /** [firstNumber]부터 붙인 번호와 바로 앞 줄(첫 줄이면 null)을 함께 넘겨 각 줄을 바꾼다. */
    fun <T> mapNumbered(firstNumber: Int = FIRST_NUMBER, transform: (Int, TranscriptTurn, TranscriptTurn?) -> T): List<T> =
        values.mapIndexed { index, turn -> transform(index + firstNumber, turn, values.getOrNull(index - 1)) }

    /** [from]번째(0부터)부터 [until]번째 앞까지의 줄. */
    fun slice(from: Int, until: Int): TranscriptTurns = TranscriptTurns(values.subList(from, until))

    /** [from]번째(0부터) 줄부터 센 본인 줄 수. */
    fun selfCountFrom(from: Int): Int = values.drop(from).count { turn -> turn.isSelf() }

    /**
     * 본문 길이가 비슷하도록 [parts]개로 나눌 때의 경계 위치(0부터). 처음 0과 끝 [size]를 포함하고 빈 구간은 없다.
     * 경계는 앞에서부터 센 본문 길이가 전체의 i/[parts]에 처음 닿는 줄이다.
     */
    fun partitionBounds(parts: Int): List<Int> {
        val cumulative = values.runningFold(0L) { total, turn -> total + turn.length() }
        val total = cumulative.last()
        val inner = (1 until parts).map { part -> cumulative.indexOfFirst { length -> length * parts >= total * part } }
        return (listOf(0) + inner + values.size).distinct().sorted()
    }

    /** 본인이 쓴 [keywords] 낱말이 나온 횟수의 합. 한 줄에 여러 번 나오면 모두 센다. */
    fun selfOccurrences(keywords: List<String>): Int =
        values.filter { turn -> turn.isSelf() }.sumOf { turn -> keywords.sumOf { keyword -> turn.text.split(keyword).size - 1 } }

    /** [index]번째(0부터) 줄이 본인 줄이면 ` / `로 이은 토막 가운데 [maxChars]자 이하인 첫 토막. 없으면 null이다. */
    fun selfQuoteAt(index: Int, maxChars: Int): String? {
        val turn = values.getOrNull(index)?.takeIf { it.isSelf() } ?: return null
        return turn.pieces().firstOrNull { piece -> piece.length in MIN_QUOTE_CHARS..maxChars }
    }

    companion object {
        /** 대화문 첫 줄의 번호. 근거 번호와 구간 번호가 모두 이 값에서 시작한다. */
        const val FIRST_NUMBER = 1

        /** 이보다 짧은 토막(ㅇㅇ, ㅋㅋ)은 한마디로 고르지 않는다. */
        private const val MIN_QUOTE_CHARS = 4
    }
}
