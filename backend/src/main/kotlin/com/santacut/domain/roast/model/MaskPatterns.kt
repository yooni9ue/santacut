package com.santacut.domain.roast.model

/** 미리 만든 고유명사 사전 정규식 목록. 앱이 뜰 때 한 번 만들어 모든 줄에 쓴다. */
class MaskPatterns(private val values: List<MaskPattern>) {
    /** [text]의 사전 낱말을 모두 일반명사 표기로 바꾼다. */
    fun maskIn(text: String): ScrubbedText {
        val masked = mutableListOf<String>()
        val replaced = values.fold(text) { current, pattern -> pattern.maskIn(current, masked) }
        return ScrubbedText(replaced, masked)
    }
}
