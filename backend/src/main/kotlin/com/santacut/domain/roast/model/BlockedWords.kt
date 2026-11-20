package com.santacut.domain.roast.model

/**
 * 한 요청의 차단 목록. 입력에서 가린 고유명사 원래 낱말이다. 출력 검증에만 쓰고 저장하지 않는다.
 */
class BlockedWords(private val values: List<String>) {
    /** [text]에 처음 나온 차단 낱말. 없으면 null이다. 긴 낱말부터 맞춰 '현규형'이 '현규'보다 먼저 걸린다. */
    fun foundIn(text: String): String? = values.distinct().sortedByDescending(String::length).find { word -> word in text }

    fun size(): Int = values.distinct().size

    companion object {
        val EMPTY = BlockedWords(emptyList())
    }
}
