package com.santacut.domain.analysis.model

/**
 * 인스타그램 내보내기 JSON은 UTF-8 바이트를 한 바이트씩 `\u00XX` 문자로 적는다. 그렇게 깨진 글자를 되살린다.
 */
object MojibakeText {
    private val BEYOND_LATIN1 = Regex("[^\\u0000-\\u00FF]")
    private const val REPLACEMENT_CHARACTER = '�'

    /**
     * 모든 문자가 U+00FF 이하이면 그 값을 바이트로 보고 UTF-8로 다시 읽는다.
     * 다시 읽은 결과가 올바른 UTF-8이 아니면(대체 문자가 생기면) 원래 문자열을 그대로 쓴다.
     */
    fun repair(text: String): String {
        if (BEYOND_LATIN1.containsMatchIn(text)) return text
        val decoded = String(text.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        return if (decoded.contains(REPLACEMENT_CHARACTER)) text else decoded
    }
}
