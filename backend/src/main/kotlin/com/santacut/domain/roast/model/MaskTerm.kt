package com.santacut.domain.roast.model

/**
 * 고유명사 사전 한 묶음(rules.json의 proper_nouns). 사전 낱말이 낱말 경계(뒤에 조사만 붙은 자리)에 나오면 [label]로 바꾼다.
 * '대만족', '성수기'처럼 사전 낱말 뒤에 다른 글자가 이어지면 바꾸지 않는다.
 *
 * @property label 바꿔 넣을 일반명사 표기. 예: (장소), (가게), (행사)
 */
data class MaskTerm(val label: String, val words: List<String>) {
    /** 낱말 경계의 사전 낱말 정규식. 조사 정규식 [particles]는 낱말 뒤에 붙어도 되는 말이다. */
    fun regex(particles: String): Regex =
        Regex("(?<![가-힣])(${words.joinToString(ALTERNATION) { word -> Regex.escape(word) }})(?=(?:$particles)?(?![가-힣]))")

    companion object {
        private const val ALTERNATION = "|"
    }
}
