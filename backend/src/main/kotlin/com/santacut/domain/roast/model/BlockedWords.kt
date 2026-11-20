package com.santacut.domain.roast.model

/**
 * 한 요청의 차단 목록. 입력에서 가린 원래 낱말(사람 이름, 사는 곳 이름, 영문)과, 호칭·부르는 말에서 떼어 낸 이름 줄기('현규형'이면 '현규형'과 '현규')다.
 * 출력 검증에만 쓰고 저장하지 않는다.
 */
class BlockedWords(private val values: List<String>) {
    /** [text]에 처음 나온 차단 낱말. 없으면 null이다. 긴 낱말부터 맞춰 '현규형'이 '현규'보다 먼저 걸린다. */
    fun foundIn(text: String): String? = values.distinct().sortedByDescending(String::length).find { word -> word in text }

    fun size(): Int = values.distinct().size

    companion object {
        val EMPTY = BlockedWords(emptyList())
    }
}
