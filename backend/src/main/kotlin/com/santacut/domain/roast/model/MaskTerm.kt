package com.santacut.domain.roast.model

/**
 * 고유명사 사전 한 묶음(rules.json의 proper_nouns). 사전 낱말이 낱말 앞 경계에 나오면 뒤에 무엇이 붙든 [label]로 바꾼다.
 * '제주여행', '홍대입구', '서산가자'처럼 붙여 쓴 말도 가린다. '대만족', '성수기'처럼 사전 낱말로 시작하는 흔한 말은 예외 목록이 건너뛴다.
 *
 * @property label 바꿔 넣을 일반명사 표기. 예: (장소), (가게), (행사)
 */
data class MaskTerm(val label: String, val words: List<String>) {
    /** 낱말 앞 경계의 사전 낱말 정규식. 앞부분이 겹치는 낱말은 긴 것부터 맞춘다. */
    fun regex(): Regex =
        Regex("(?<![가-힣])(${words.sortedByDescending(String::length).joinToString(ALTERNATION) { word -> Regex.escape(word) }})")

    companion object {
        private const val ALTERNATION = "|"
    }
}
