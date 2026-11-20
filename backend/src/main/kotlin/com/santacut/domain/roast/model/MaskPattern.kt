package com.santacut.domain.roast.model

/** 미리 만든 이름 줄기 정규식 하나와 바꿔 넣을 표기. */
class MaskPattern(private val regex: Regex, private val label: String) {
    /** [text]에서 맞은 낱말을 [label]로 바꾸고, 바꾼 원래 낱말을 [masked]에 더한다. */
    fun maskIn(text: String, masked: MutableList<String>): String =
        regex.replace(text) { match ->
            masked += match.value
            label
        }
}
