package com.santacut.domain.roast.model

/**
 * 한 요청의 차단 목록. 입력에서 가린 원래 낱말(사람 이름, 사는 곳 이름, 영문)과, 호칭·부르는 말에서 떼어 낸 이름 줄기('현규형'이면 '현규형'과 '현규')다.
 * 출력 검증에만 쓰고 저장하지 않는다. 맞출 정규식은 만들 때 한 번 만든다.
 */
class BlockedWords(private val values: List<String>) {
    /** 긴 낱말부터 맞추는 낱말 앞 경계 정규식. 낱말이 없으면 null이다. */
    private val pattern: Regex? = values.distinct().sortedByDescending(String::length).takeIf(List<String>::isNotEmpty)
        ?.let { words -> Regex(LEADING_BOUNDARY + "(?:" + words.joinToString(ALTERNATION, transform = Regex::escape) + ")") }

    /**
     * [text]에서 낱말 앞 경계(앞에 한글이 붙지 않은 자리)에 처음 나온 차단 낱말. 없으면 null이다. 긴 낱말부터 맞춰 '현규형'이 '현규'보다 먼저 걸린다.
     * 입력에서 가릴 때와 같은 경계라 이름이 '하나'여도 '두하나' 같은 낱말 속 글자는 막지 않는다.
     */
    fun foundIn(text: String): String? = pattern?.find(text)?.value

    fun size(): Int = values.distinct().size

    /** [words]를 더한 차단 목록. */
    fun plus(words: List<String>): BlockedWords = BlockedWords(values + words)

    companion object {
        val EMPTY = BlockedWords(emptyList())
        private const val LEADING_BOUNDARY = "(?<![가-힣])"
        private const val ALTERNATION = "|"
    }
}
