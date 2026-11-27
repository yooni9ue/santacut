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
        /** 따옴표·낫표 인용. 곧은·둥근 작은따옴표와 큰따옴표, 홑·겹낫표를 본다. 유행어 세기도 이 인용 밖만 본다([RoastRules.slangCountIn]). */
        val QUOTE = Regex("'([^'\\n]*)'|‘([^’\\n]*)’|\"([^\"\\n]*)\"|“([^”\\n]*)”|「([^」\\n]*)」|『([^』\\n]*)』")
        private const val PARENTHESES = "()"
        private val HANGUL_RUN = Regex("[가-힣]{2,}")

        /** 다른 구간의 표현을 보는 문장 종류. 구간을 말하는 판정 한 줄, 칭호, 마지막 한 줄이다. */
        private val BAND_KINDS = setOf(RoastTextKind.VERDICT_LINE, RoastTextKind.TITLE, RoastTextKind.FINAL_LINE)

        /** 어느 덱에나 붙는 낱말의 앞 두 글자. 이 낱말만으로는 근거와 겹친 것으로 치지 않는다(칩 뜻 풀이의 말풍선·보낸도 여기 든다). */
        private val GENERIC_STEMS = setOf("산타", "올해", "이건", "그건", "이거", "그거", "인정", "장부", "말풍", "보낸", "보냈")
        private const val GENERIC_STEM_LENGTH = 2
        private const val GENERIC = "일반론(말풍선·칩과 겹치는 낱말이 없다)"
        private const val NOT_PUNCH = "밈 펀치가 아니다(slang 표기나 펀치 틀 표지가 없다)"
        private const val FLAT_ENDING = "끝이 밋밋하다(~함·~음·~봄 같은 요약이나 명사로 끝내지 말고 완결된 문장으로 찌른다)"
        private const val DESCRIPTIVE_TITLE = "칭호가 서술형이다(밈형 명사구로 쓴다)"
        private const val COPIED_EXAMPLE = "예시 문장을 그대로 옮겼다(이 사람의 말풍선과 칩으로 새로 쓴다)"
        private const val EMPTY_VERDICT_SLOT = "판정 자리가 비었거나 입버릇만 넣었다(판정을 말하는 낱말로 채운다)"
        private const val RESTATED_CHIP = "칩 이름을 그대로 옮겼다(칩이 센 버릇의 뜻을 말한다)"

        /** 판정 자리의 낱말이 입버릇 뒤에 붙여도 되는 조사 길이. '출발이'도 입버릇 '출발'로 본다. */
        private const val PARTICLE_LENGTH = 1
        private const val SPACE = " "

        /** 라벨 안에서 이만큼 이어진 음절이 말풍선에 그대로 있으면 베낀 라벨로 본다. */
        const val COPIED_LABEL_LENGTH = 3

        /** 앞 카드 문장과 이만큼 이상 같은 한글 낱말이 겹치면 되풀이로 본다. */
        private const val REPEATED_WORD_LENGTH = 3

        /** 이만큼의 카드(이 카드 포함)에 나오면 짧은 낱말도 되풀이로 본다. */
        private const val REPEATED_CARD_COUNT = 3

        /** 되풀이로 치지 않는 흔한 부사·대명사·서술어. 카드마다 나와도 틀에 찍은 문장으로 읽히지 않는다. */
        private val REPEAT_STOPWORDS = setOf(
            "진짜", "그냥", "매번", "항상", "이미", "아직", "벌써", "결국", "역시", "완전", "너는", "네가",
            "있다", "없다", "했다", "한다", "이다", "거다", "거야", "오늘도", "수준이다",
        )

        /** 라벨끼리 이만큼의 첫머리 음절이 같으면 겹친 라벨로 본다. */
        private const val LABEL_HEAD_LENGTH = 3

        /** 끝 표시(마침표, 물결표, 말줄임표, 따옴표, 공백)를 뗀 문장 끝. */
        private val TRAILING_MARKS = Regex("[\\s.,~…?!'\"‘’“”]+$")

        /** 따옴표 인용으로 끝나는 문장(끝의 마침표·공백만 뗀다). "약속은 늘 '빨리'."처럼 인용으로 맺으면 명사 끝처럼 밋밋하다. */
        private val QUOTE_ENDING = Regex("['\"’”」』][\\s.]*$")
        private const val HANGUL_FIRST = 0xAC00
        private const val HANGUL_LAST = 0xD7A3
        private const val JONGSEONG_COUNT = 28

        /** 받침 ㅁ의 종성 번호. ~함·~음·~봄·~챙김처럼 명사형으로 요약한 끝이 이 받침으로 끝난다. */
        private const val JONGSEONG_MIEUM = 16

        /** 받침이 있어도 문장 끝일 수 있는 종성: ㄴ(~군, ~건), ㄹ(~걸, ~할), ㅆ(~했·~었 줄임). 그 밖의 받침 끝은 명사(타입, 버릇, 폭격)로 본다. */
        private val SENTENCE_JONGSEONG = setOf(4, 8, 20)

        /** 받침 없이 끝나도 명사형인 끝 음절(~하기). */
        private val NOMINAL_ENDINGS = setOf('기')

        /** 칭호를 서술형으로 만드는 끝 음절(~다, ~요, ~함, ~음 등)과 관형형 낱말 끝(~하는, ~했던, ~할). */
        private val PREDICATE_TITLE_ENDINGS = setOf('다', '요', '함', '음', '됨', '임', '냐', '니', '지', '네', '까', '죠')
        private val ADNOMINAL_WORD = Regex("[가-힣]+(?:는|던) ")

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

        /**
         * 작가 문장 [text]가 예시 문장(프롬프트 예시, 서버 대체 문장, 유행어 예문, [RoastBrief.isExample])을 그대로 옮겼는지.
         * 대체 문장 자체는 서버가 쓰는 문장이라 이 검사를 하지 않고, 작가 응답 필드에만 쓴다.
         */
        fun copiedExample(text: String, brief: RoastBrief): TextProblems = TextProblems(listOfNotNull(COPIED_EXAMPLE.takeIf { brief.isExample(text) }))

        /**
         * 문장 [text]가 칩이 말하지 않는 시간대·요일을 말하는지. 시간대 낱말([RoastRules.timeWords])은 칩 글([sources]: 칩 이름, 칩 값, 버릇 낱말)이나
         * 고른 말풍선 [quote]에 있을 때만 쓴다. 공유 말풍선에 피크타임 칩을 붙이고 '점심마다 공유 폭격'이라 쓰는 것처럼 칩에 없는 시간 버릇을 지어내지 않게 한다.
         */
        fun timeClaims(text: String, rules: RoastRules, sources: List<String>, quote: String?): TextProblems {
            val stated = sources + listOfNotNull(quote)
            val claimed = rules.timeWordsIn(text).firstOrNull { word -> stated.none { source -> word in source } }
            return TextProblems(listOfNotNull(claimed?.let { word -> "칩이 말하지 않는 시간대($word)" }))
        }

        /**
         * 문장 [text]가 지표 칩 이름 [chipLabel]을 띄어쓰기만 바꿔 그대로 옮겼는지. '평균 길이 6.9자' 칩에 '평균 길이 짧게 툭 던진다'처럼
         * 칩을 되읽는 한 줄은 버릇을 찌르지 못한다. 칩 이름이 버릇 낱말인 칩('따뜻한 말')은 [RoastBrief.isRestatable]이 빼고 넘긴다.
         */
        fun restated(text: String, chipLabel: String?): TextProblems {
            val label = chipLabel?.replace(SPACE, "")?.takeIf(String::isNotEmpty) ?: return TextProblems(emptyList())
            return TextProblems(listOfNotNull(RESTATED_CHIP.takeIf { label in text.replace(SPACE, "") }))
        }

        /**
         * 라벨·칭호 [text]가 앞 라벨·칭호 [earlier]와 덱 되풀이 금지 줄기([RoastRules.repeatStems]: 발사, 품절, 가뭄 등)를 함께 쓰는지.
         * '감사발사상', '약속발사상', '뭐임발사대'처럼 한 덱에 같은 줄기가 거듭 찍히지 않게 한다.
         */
        fun repeatedStem(text: String, earlier: List<String>, rules: RoastRules): TextProblems {
            val used = earlier.flatMap(rules::repeatStemsIn).toSet()
            val stem = rules.repeatStemsIn(text).firstOrNull(used::contains)
            return TextProblems(listOfNotNull(stem?.let { found -> "덱 안에서 같은 줄기를 되풀이했다($found)" }))
        }

        /**
         * 라벨 [label]이 말풍선 [quote]를 그대로 옮겼는지. 상 꼴 끝([suffix])을 뗀 라벨 안의 [COPIED_LABEL_LENGTH]음절 이상 구간이 말풍선(띄어쓰기 무시)에
         * 그대로 있으면 떨어진다. '고마워 덕분에 살았다'에 '살았다상'처럼 말풍선을 베낀 라벨은 상 이름이 아니라 말풍선 되풀이다. 말풍선이 없으면 보지 않는다.
         */
        fun copiedLabel(label: String, suffix: String, quote: String?): TextProblems {
            val bareQuote = quote?.replace(SPACE, "") ?: return TextProblems(emptyList())
            val stem = labelStemOf(label, suffix)
            val copied = stem.windowed(COPIED_LABEL_LENGTH).firstOrNull { window -> window in bareQuote }
            return TextProblems(listOfNotNull(copied?.let { found -> "라벨이 말풍선을 그대로 옮겼다($found)" }))
        }

        /**
         * 근거 카드 [text]가 앞 카드 문장 [earlier]의 내용 낱말을 되풀이했는지. 따옴표로 든 입버릇과 이 카드 칩 글([chipTexts])에 있는 낱말은 빼고 본다.
         * 덱을 넘겨 볼 때 같은 말이 카드마다 되풀이되면 틀에 찍은 문장으로 읽힌다. 다만 흔한 말([REPEAT_STOPWORDS])은 보지 않고,
         * [REPEATED_WORD_LENGTH]음절 이상 낱말은 두 카드부터, 짧은 낱말('버릇'·'방을')은 [REPEATED_CARD_COUNT]카드에 나올 때만 되풀이로 본다.
         */
        fun repeated(text: String, earlier: List<String>, chipTexts: List<String>): TextProblems {
            val own = chipTexts.flatMap(::contentWordsOf).toSet() + REPEAT_STOPWORDS
            val previous = earlier.flatMap { other -> contentWordsOf(QUOTE.replace(other, "")).toSet() }.groupingBy { word -> word }.eachCount()
            val word = contentWordsOf(QUOTE.replace(text, "")).firstOrNull { candidate ->
                val seen = previous[candidate] ?: 0
                candidate !in own && seen > 0 && (candidate.length >= REPEATED_WORD_LENGTH || seen + 1 >= REPEATED_CARD_COUNT)
            }
            return TextProblems(listOfNotNull(word?.let { found -> "앞 카드와 같은 낱말($found)" }))
        }

        /**
         * 라벨 [label]이 앞 카드 라벨 [earlier]나 판정 한 줄 [verdictLine]과 겹치는지. 상 꼴 끝([suffix])과 띄어쓰기를 뗀 라벨의 앞 [LABEL_HEAD_LENGTH]음절이
         * 앞 라벨과 같거나(답장독촉상·답장지연상), 라벨 전체가 판정 한 줄에 그대로 있으면('답장 독촉 혐의로 체포하겠어'에 답장독촉상) 떨어진다.
         */
        fun sharedLabel(label: String, earlier: List<String>, verdictLine: String, suffix: String): TextProblems {
            val stem = labelStemOf(label, suffix)
            val head = stem.take(LABEL_HEAD_LENGTH)
            val repeated = head.length == LABEL_HEAD_LENGTH && earlier.any { other -> labelStemOf(other, suffix).take(LABEL_HEAD_LENGTH) == head }
            val inVerdict = stem.length >= LABEL_HEAD_LENGTH && stem in verdictLine.replace(SPACE, "")
            return TextProblems(
                listOfNotNull(
                    "앞 카드 라벨과 첫머리가 같다($head)".takeIf { repeated },
                    "판정 한 줄을 그대로 옮긴 라벨($stem)".takeIf { inVerdict },
                ),
            )
        }

        private fun labelStemOf(label: String, suffix: String): String = label.trim().removeSuffix(suffix).replace(SPACE, "")

        /**
         * 근거 한 줄 [text]의 끝이 완결된 찌르기 문장인지. 따옴표 인용으로 끝나거나, 끝 표시를 뗀 마지막 글자가 한글 음절이 아니거나,
         * 받침 ㅁ(~함·~음·~봄·~챙김 같은 요약)이나 문장 끝이 될 수 없는 받침(명사 끝: 타입, 버릇, 폭격), 명사형 끝(~하기)이면 밋밋하다.
         * 허용 유행어가 들어 있으면('질문으로 판 깔기, 요이~') 유행어가 펀치를 맡으므로 보지 않는다.
         */
        fun isFlatEnding(text: String, rules: RoastRules): Boolean {
            if (rules.slangCountIn(text) > 0) return false
            if (QUOTE_ENDING.containsMatchIn(text)) return true
            val last = TRAILING_MARKS.replace(text, "").lastOrNull() ?: return true
            if (last.code !in HANGUL_FIRST..HANGUL_LAST) return true
            val jongseong = (last.code - HANGUL_FIRST) % JONGSEONG_COUNT
            return jongseong == JONGSEONG_MIEUM || (jongseong != 0 && jongseong !in SENTENCE_JONGSEONG) || last in NOMINAL_ENDINGS
        }

        /** 칭호 [text]가 서술형인지. 끝 표시가 붙었거나, 서술형 끝 음절로 끝나거나, 관형형 낱말(~하는, ~했던)로 꾸민 구다. */
        fun isDescriptiveTitle(text: String): Boolean {
            val trimmed = text.trim()
            val last = trimmed.lastOrNull() ?: return false
            return TRAILING_MARKS.containsMatchIn(trimmed) || last in PREDICATE_TITLE_ENDINGS || ADNOMINAL_WORD.containsMatchIn(trimmed)
        }

        private fun contentWordsOf(text: String): List<String> =
            HANGUL_RUN.findAll(text).map { match -> match.value }.filterNot { word -> word.take(GENERIC_STEM_LENGTH) in GENERIC_STEMS }.toList()

        private fun quotedIn(text: String): List<String> =
            QUOTE.findAll(text).map { match -> match.groupValues.drop(1).firstOrNull(String::isNotEmpty).orEmpty() }.toList()

        /**
         * [kind] 문장 [text]를 검사한다. 글자 수, 숫자·셈말 표현, 금지 말투(옛말투·유행어·이모지·느낌표·ㅋㅋ, 따옴표 인용 밖만), 칭호 금지어,
         * 필드마다 하나를 넘은 유행어, 판정 방향, 칭찬 뒤집기 말(근데·덕분에·그래도, 따옴표로 든 본인 입버릇 '근데'는 제외), 상 이름 꼴이 아닌 라벨, 밈 펀치가 아닌 결론 한 줄([RoastRules.isPunch]),
         * 추천 펀치 틀을 쓰지 않은 결론 한 줄, 끝이 밋밋한 근거 한 줄([isFlatEnding]), 서술형 칭호([isDescriptiveTitle]),
         * 금지어(영포티 말투 포함), 가린 낱말(이름 등), 알파벳(허용 유행어 안 제외), 괄호 표기, 이름·사는 곳 의심, 목록 밖 인용, 본인 말풍선을 길게 베낀 말을 본다.
         * 다른 구간의 표현([RoastBrief.foreignMarkIn])은 판정 한 줄·칭호·마지막 한 줄에서 떨어진다. 예시 베끼기는 작가 문장에만 [copiedExample]로 본다.
         * 판정 한 줄은 정해 준 판정 틀, 빈 판정 자리, 고마워 칩 말도 본다([verdictProblems]).
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
            rules.flipIn(QUOTE.replace(text, ""))?.let { found -> "칭찬 뒤집기 말($found)" },
            NOT_PUNCH.takeIf { kind == RoastTextKind.FINAL_LINE && !rules.isPunch(text) },
            punchTemplateProblem(text, kind, brief, rules),
            FLAT_ENDING.takeIf { kind == RoastTextKind.EVIDENCE_LINE && isFlatEnding(text, rules) },
            DESCRIPTIVE_TITLE.takeIf { kind == RoastTextKind.TITLE && isDescriptiveTitle(text) },
            brief.foreignMarkIn(text)?.takeIf { kind in BAND_KINDS }?.let { mark -> "다른 구간의 표현($mark)" },
            "라벨은 '${rules.labelSuffix}'으로 끝나는 상 이름이어야 한다".takeIf { kind == RoastTextKind.LABEL && !rules.isAwardLabel(text) },
            rules.labelShapeProblemIn(text)?.takeIf { kind == RoastTextKind.LABEL },
            rules.personalityIn(text)?.let { found -> "인격 단정어($found, 놀림은 행동에만 한다)" },
            rules.stockPhraseIn(text)?.let { found -> "예시 상투 문구($found)" },
        ) + titleProblems(text, kind, brief, rules) + verdictProblems(text, kind, brief, rules)

        /**
         * 칭호만의 검사. 금지 끝말(왕, 장인)로 끝나거나, 이 사람 구간 이름을 되풀이하거나('명단 실종자'), 컷과 붙지 않았는데 턱걸이 같은 아슬아슬하다는 말을 쓰면 떨어진다.
         */
        private fun titleProblems(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): List<String> {
            if (kind != RoastTextKind.TITLE) return emptyList()
            return listOfNotNull(
                rules.titleEndingIn(text)?.let { found -> "칭호 끝말 금지($found)" },
                brief.bandNameIn(text)?.let { found -> "구간 이름을 되풀이한 칭호($found)" },
                brief.farNearMissIn(text, rules)?.let { found -> "점수와 어긋난 칭호($found, 컷과 붙지 않았다)" },
            )
        }

        /**
         * 판정 한 줄만의 검사. 정해 준 판정 틀([RoastBrief.verdictPunch])의 표지를 써야 하고, 판정 자리를 입버릇 하나로 채우거나('출발 줴줴이야~') 비우면 안 되며,
         * 고마워 칩이 있을 때 고맙다는 말로 판정을 이끌지 않는다('감사 품절. L을 가져가.').
         */
        private fun verdictProblems(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): List<String> {
            if (kind != RoastTextKind.VERDICT_LINE) return emptyList()
            val punch = brief.verdictPunch
            return listOfNotNull(
                punch?.takeUnless { template -> template.isUsedIn(text, rules) }?.let { template -> "정해 준 판정 틀(verdict_template)을 쓰지 않았다(표지 ${template.marker})" },
                EMPTY_VERDICT_SLOT.takeIf { hasEmptySlot(text, brief, rules) },
                brief.thanksWordIn(text)?.let { word -> "판정 한 줄은 고마워 칩으로 이끌지 않는다($word)" },
            )
        }

        /** 펀치 표지와 따옴표 인용을 뺀 판정 한 줄에 입버릇이 아닌 두 글자 이상 낱말이 하나도 없는지. */
        private fun hasEmptySlot(text: String, brief: RoastBrief, rules: RoastRules): Boolean {
            val bare = rules.punchMarksIn(text).fold(QUOTE.replace(text, "")) { rest, mark -> rest.replace(mark, SPACE) }
            val tokens = brief.phraseTokens()
            return HANGUL_RUN.findAll(bare).map { match -> match.value }.none { word ->
                tokens.none { token -> word.startsWith(token) && word.length - token.length <= PARTICLE_LENGTH }
            }
        }

        /** 마지막 한 줄이 이 사람에게 추천한 펀치 틀([RoastBrief.suggestedPunches]) 가운데 하나를 썼는지. 추천이 없으면 보지 않는다. */
        private fun punchTemplateProblem(text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): String? {
            if (kind != RoastTextKind.FINAL_LINE || !rules.isPunch(text) || brief.suggestedPunches.anyUsedIn(text, rules)) return null
            return "추천 펀치 틀(punch_templates)을 쓰지 않았다(표지 ${brief.suggestedPunches.markers().joinToString(", ")} 가운데 하나)"
        }

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
