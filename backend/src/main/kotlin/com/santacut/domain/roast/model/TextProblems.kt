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
        /** 따옴표·낫표 인용. 곧은·둥근 작은따옴표와 큰따옴표, 홑·겹낫표를 본다. */
        private val QUOTE = Regex("'([^'\\n]*)'|‘([^’\\n]*)’|\"([^\"\\n]*)\"|“([^”\\n]*)”|「([^」\\n]*)」|『([^』\\n]*)』")
        private const val PARENTHESES = "()"
        private val HANGUL_RUN = Regex("[가-힣]{2,}")

        /** 어느 덱에나 붙는 낱말의 앞 두 글자. 이 낱말만으로는 근거와 겹친 것으로 치지 않는다(칩 뜻 풀이의 말풍선·보낸도 여기 든다). */
        private val GENERIC_STEMS = setOf("산타", "올해", "이건", "그건", "이거", "그거", "인정", "장부", "말풍", "보낸", "보냈")
        private const val GENERIC_STEM_LENGTH = 2
        private const val GENERIC = "일반론(말풍선·칩과 겹치는 낱말이 없다)"
        private const val NOT_PUNCH = "밈 펀치가 아니다(slang 표기나 펀치 틀 표지가 없다)"

        /**
         * 일반론 검사. 문장 [text]가 고른 말풍선 표시 [quote]나 칩 글([chipTexts]: 칩 이름, 칩 뜻 풀이)과 내용 낱말을 하나 이상 나눠야 한다.
         * 내용 낱말은 [GENERIC_STEMS]로 시작하지 않는 두 글자 이상 한글 낱말이다. 활용으로 둘째 글자가 바뀌어도(고마워·고맙다) 맞게
         * 첫 글자가 같은 근거 낱말이 있으면 겹친 것으로 본다. 말풍선 낱말은 첫 글자가 그 안 어디에 있어도 된다(대답좀·답장).
         * 근거에 따옴표로 든 입버릇('ㄱ?')을 그대로 써도 겹친 것이다. 근거에 내용 낱말이 하나도 없으면(ㅋㅋ만 있는 말풍선 등) 보지 않는다.
         */
        fun grounding(text: String, quote: String?, chipTexts: List<String>): TextProblems {
            val quoteWords = quote?.let(::contentWordsOf).orEmpty()
            val sourceWords = quoteWords + chipTexts.flatMap(::contentWordsOf)
            val phrases = (listOfNotNull(quote) + chipTexts).flatMap(::quotedIn).filter(String::isNotBlank)
            if (sourceWords.isEmpty() && phrases.isEmpty()) return TextProblems(emptyList())
            val shared = contentWordsOf(text).map(String::first).any { initial ->
                sourceWords.any { word -> word.first() == initial } || quoteWords.any { word -> initial in word }
            } || phrases.any(text::contains)
            return TextProblems(listOfNotNull(GENERIC.takeUnless { shared }))
        }

        private fun contentWordsOf(text: String): List<String> =
            HANGUL_RUN.findAll(text).map { match -> match.value }.filterNot { word -> word.take(GENERIC_STEM_LENGTH) in GENERIC_STEMS }.toList()

        private fun quotedIn(text: String): List<String> =
            QUOTE.findAll(text).map { match -> match.groupValues.drop(1).firstOrNull(String::isNotEmpty).orEmpty() }.toList()

        /**
         * [kind] 문장 [text]를 검사한다. 글자 수, 숫자·셈말 표현, 금지 말투(옛말투·유행어·이모지·느낌표·ㅋㅋ, 따옴표 인용 밖만), 칭호 금지어,
         * 필드마다 하나를 넘은 유행어, 판정 방향, 칭찬 뒤집기 말(근데·덕분에·그래도), 상 이름 꼴이 아닌 라벨, 밈 펀치가 아닌 결론 한 줄([RoastRules.isPunch]),
         * 금지어(영포티 말투 포함), 가린 낱말(이름 등), 알파벳(허용 유행어 안 제외), 괄호 표기, 이름·사는 곳 의심, 목록 밖 인용, 본인 말풍선을 길게 베낀 말을 본다.
         */
        fun of(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): TextProblems =
            TextProblems(wordingProblems(text, kind, brief, rules) + safetyProblems(text, brief, rules) + quoteProblems(text, brief))

        private fun wordingProblems(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): List<String> = listOfNotNull(
            kind.lengthProblemOf(text),
            rules.numberIn(text)?.let { found -> "숫자 표현($found)" },
            rules.toneIn(QUOTE.replace(text, ""))?.let { found -> "금지 말투($found)" },
            rules.titleBannedIn(text)?.takeIf { kind == RoastTextKind.TITLE }?.let { found -> "칭호 금지어($found)" },
            rules.slangOverflowIn(text)?.let { found -> "유행어는 필드마다 하나까지($found)" },
            rules.wrongDirectionIn(text, brief.verdict.passed)?.let { found -> "판정과 어긋남($found)" },
            rules.flipIn(text)?.let { found -> "칭찬 뒤집기 말($found)" },
            "라벨은 '${rules.labelSuffix}'으로 끝나는 상 이름이어야 한다".takeIf { kind == RoastTextKind.LABEL && !rules.isAwardLabel(text) },
            NOT_PUNCH.takeIf { kind == RoastTextKind.FINAL_LINE && !rules.isPunch(text) },
        )

        private fun safetyProblems(text: String, brief: RoastBrief, rules: RoastRules): List<String> = listOfNotNull(
            rules.bannedIn(text)?.let { found -> "금지어($found)" },
            brief.blockedIn(text)?.let { found -> "가린 낱말($found)" },
            "알파벳 낱말".takeIf { rules.hasLatin(text) },
            "괄호 표기".takeIf { text.any { character -> character in PARENTHESES } },
            rules.suspectsIn(text).firstOrNull()?.let { found -> "이름 의심($found)" },
        )

        private fun quoteProblems(text: String, brief: RoastBrief): List<String> = quotedIn(text)
            .filterNot { quote -> brief.isQuotable(quote) }
            .map { quote -> "목록 밖 인용($quote)" }
            .toList() + listOfNotNull(brief.copiedIn(text)?.let { copied -> "말풍선 베낌($copied)" })
    }
}
