package com.santacut.domain.roast.model

/**
 * 미리 만든 고유명사 정규식 하나와 바꿔 넣을 표기. 맞은 자리가 예외 [exceptions] 가운데 하나로 시작하면 바꾸지 않는다.
 */
class MaskPattern(private val regex: Regex, private val label: String, private val exceptions: List<String> = emptyList()) {
    /** [text]에서 맞은 낱말을 [label]로 바꾸고, 바꾼 원래 낱말을 [masked]에 더한다. */
    fun maskIn(text: String, masked: MutableList<String>): String =
        regex.replace(text) { match ->
            if (exceptions.any { exception -> text.startsWith(exception, match.range.first) }) return@replace match.value
            masked += match.value
            label
        }
}
