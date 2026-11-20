package com.santacut.domain.roast.model

/**
 * 작가 문장 하나가 검증에 떨어진 사유 목록. 비어 있으면 통과다.
 * 사유는 다시 쓰기 요청과 검증 결과에만 쓰고 로그에는 남기지 않는다(로그에는 필드 이름과 수만 남긴다).
 */
class TextProblems(private val values: List<String>) {
    fun toList(): List<String> = values.toList()

    /** 사유마다 앞에 [label]을 붙인다. 짝처럼 한 필드에 문장이 둘일 때 어느 쪽인지 알린다. */
    fun labeled(label: String): List<String> = values.map { reason -> "$label: $reason" }

    companion object {
        /** 작은따옴표 인용(곧은 따옴표와 둥근 따옴표). */
        private val QUOTE = Regex("'([^'\\n]*)'|‘([^’\\n]*)’")
        private const val PARENTHESES = "()"

        /**
         * [kind] 문장 [text]를 검사한다. 글자 수, 숫자·수량 표현, 금지어, 가린 고유명사, 알파벳, 괄호 표기, 고유명사 의심,
         * 목록 밖 인용, 판정 방향, 담백 판결의 석탄·탈락, "근데"로 여는 뒤집기를 본다.
         */
        fun of(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): TextProblems =
            TextProblems(wordingProblems(text, kind, brief, rules) + safetyProblems(text, brief, rules) + quoteProblems(text, brief))

        private fun wordingProblems(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): List<String> = listOfNotNull(
            kind.lengthProblemOf(text),
            rules.numberIn(text)?.let { found -> "숫자 표현($found)" },
            rules.wrongDirectionIn(text, brief.verdict.passed)?.let { found -> "판정과 어긋남($found)" },
            rules.mildWrongIn(text)?.takeIf { kind == RoastTextKind.VERDICT_MILD }?.let { found -> "담백 판결 금지어($found)" },
            "\"근데\"로 시작".takeIf { kind == RoastTextKind.FLIP && rules.startsFlipWrongly(text) },
        )

        private fun safetyProblems(text: String, brief: RoastBrief, rules: RoastRules): List<String> = listOfNotNull(
            rules.bannedIn(text)?.let { found -> "금지어($found)" },
            brief.blockedIn(text)?.let { found -> "가린 고유명사($found)" },
            "알파벳 낱말".takeIf { rules.hasLatin(text) },
            "괄호 표기".takeIf { text.any { character -> character in PARENTHESES } },
            rules.suspectsIn(text).firstOrNull()?.let { found -> "고유명사 의심($found)" },
        )

        private fun quoteProblems(text: String, brief: RoastBrief): List<String> = QUOTE.findAll(text)
            .map { match -> match.groupValues[1].ifEmpty { match.groupValues[2] } }
            .filterNot { quote -> brief.isQuotable(quote) }
            .map { quote -> "목록 밖 인용($quote)" }
            .toList()
    }
}
