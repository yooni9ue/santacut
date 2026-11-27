package com.santacut.domain.roast.model

/**
 * 작가가 쓰는 리캡 덱 문장 종류와 글자 수 범위(공백 포함, 코드 포인트).
 * 판정 한 줄 18자, 근거 라벨 8자, 근거 한 줄 20자, 결론 칭호 8자, 결론 한 줄 22자가 상한이다.
 */
enum class RoastTextKind(val minLength: Int, val maxLength: Int) {
    VERDICT_LINE(6, 18),
    LABEL(3, 8),
    EVIDENCE_LINE(6, 20),
    TITLE(2, 8),
    FINAL_LINE(6, 22),
    ;

    /** [text]의 글자 수가 범위 밖이면 그 사유, 안이면 null이다. */
    fun lengthProblemOf(text: String): String? {
        val length = text.codePointCount(0, text.length)
        return if (length in minLength..maxLength) null else "글자 수 ${length}자(${minLength}~${maxLength}자)"
    }
}
