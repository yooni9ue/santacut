package com.santacut.domain.roast.model

/**
 * 한 사람의 구간이 쓸 수 없는 다른 구간의 표현 목록([BandMarks.foreignTo]). 판정 한 줄·결론 칭호·마지막 한 줄 검사에 쓰고, 작가 입력에도 싣는다.
 * 본인 입버릇을 따옴표로 든 것은 작가 말이 아니라 보지 않는다.
 */
class ForeignMarks(private val values: List<String>) {
    /** 작가 입력의 banned_marks로 싣는 표현 목록. */
    fun toList(): List<String> = values.toList()

    /** [text]의 따옴표 인용 밖에 처음 나온 다른 구간 표현. 없으면 null이다. */
    fun foundIn(text: String): String? {
        val bare = TextProblems.QUOTE.replace(text, "")
        return values.firstOrNull { mark -> mark in bare }
    }

    companion object {
        val EMPTY = ForeignMarks(emptyList())
    }
}
