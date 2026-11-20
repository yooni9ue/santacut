package com.santacut.domain.roast.model

/**
 * 작가가 쓰는 문장 종류와 글자 수 범위(공백 포함, 코드 포인트).
 * 뒤집기는 꼭 여는 "근데 "(세 자)가 글자 수에 들어가므로 상한을 놀림보다 세 자 높게 둔다.
 */
enum class RoastTextKind(val minLength: Int, val maxLength: Int) {
    TITLE(8, 18),
    VERDICT(18, 42),
    VERDICT_MILD(18, 42),
    ROAST(18, 40),
    FLIP(18, 43),
    LETTER(60, 120),
    STORY(16, 34),
    ;

    /** [text]의 글자 수가 범위 밖이면 그 사유, 안이면 null이다. */
    fun lengthProblemOf(text: String): String? {
        val length = text.codePointCount(0, text.length)
        return if (length in minLength..maxLength) null else "글자 수 ${length}자(${minLength}~${maxLength}자)"
    }
}
