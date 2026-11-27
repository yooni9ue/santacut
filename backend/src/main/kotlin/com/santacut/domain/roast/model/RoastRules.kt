package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 팩폭 작가의 입력 안전장치와 출력 검증 규칙(roast/rules.json). 목록은 앱이 뜰 때 정규식으로 한 번 바꿔 둔다.
 *
 * 입력: 줄을 통째로 빼지는 않는다(사람 이름만 가린다는 결정). 영문 낱말은 (영문),
 * 숫자열은 (숫자), 사람 이름은 호칭([honorificSuffixes] 등) 패턴과 호칭 없이 부른 이름([NameStems]) 규칙으로 (이름), 아파트·빌라 같은
 * 사는 곳 이름([residenceSuffixes])은 (집)으로 바꾼다. 지명·가게·행사 이름은 가리지 않는다(사람 이름만 가린다는 결정).
 * 접미사 패턴은 오탐이 잦은 낱말([suffixExceptions])을 건너뛴다.
 * 출력: 숫자·셈말 표현([numberPatterns], 허용 [allowedNumberPhrases]), 금지어([bannedWords]), 옛말투·유행어·이모지·느낌표·ㅋㅋ([tonePatterns]),
 * 판정 방향([coalForbidden], [giftForbidden])과 칭찬 뒤집기 말([flipWords])을 막는다.
 *
 * @property residenceSuffixes 줄기 한 글자 이상에 붙으면 사는 곳(주소에 가까운 단지) 이름으로 보는 접미사(아파트, 빌라 등)
 * @property honorificSuffixes 붙으면 사람 이름으로 보는 호칭(누나, 언니 등). 호칭만 있어도 가린다.
 * @property honorificShortSuffixes 줄기 두 글자 이상에 붙으면 사람 이름으로 보는 한 글자 호칭(형, 님, 씨)
 * @property particles 낱말 뒤에 붙어도 같은 낱말로 보는 조사
 * @property nameSurnames 세 글자 이름 줄기로 볼 첫 글자(흔한 성)
 * @property nameExceptions 이름 줄기로 보지 않는 흔한 말(친구, 얘들, 사람 등)
 * @property nameTitles 참여자 표시 이름 끝에 붙여 쓰면 떼는 직함(대리, 과장 등)
 * @property unquotablePhrases quotable로 넘기지 않는 입버릇 key(비속어, 사람 부르는 말). 조서 통계에는 그대로 남는다.
 * @property quotableMaxLength quotable로 넘기는 입버릇 표기의 최대 글자 수
 * @property excludedTopics 화제 후보로 넘기지 않는 조서 주제 key. 직장 이야기도 보낸다는 결정에 따라 지금은 비어 있다.
 * @property allowedNumberPhrases 숫자 검사 전에 빼는 관용 표현. 붙여 쓴 "한마디"(짧은 말이라는 한 낱말)는 허용하고, 띄어 쓴 "한 마디"는 셈말로 본다.
 * @property tonePatterns 쓰면 안 되는 말투 정규식(옛말투 어미, 훈화 낱말, 유행어, 이모지, 느낌표, ㅋㅋ). 따옴표 인용 안은 보지 않는다.
 * 옛 명령형은 줄여 쓰는 꼴이 있는 것(두어라→둬라, 보아라→봐라, 하여라→해라)과 ~거라·오너라만 막고, 봐라·해라·먹어라 같은 반말 명령은 둔다.
 * @property titleBannedWords 결론 칭호에만 막는 말(요정, 천사, 분위기 메이커). 누구에게나 붙는 칭호라 이 사람의 자리를 말하지 못한다.
 * @property flipWords 칭찬 뒤집기로 읽히는 말(근데, 덕분에, 그래도). 리캡 덱은 칭찬 뒤집기를 쓰지 않는다.
 * @property labelSuffix 근거 카드 라벨이 끝나야 하는 글자("상"). 답장독촉상처럼 상 이름 꼴로만 쓴다.
 * @property slang 유행어 목록(roast/slang.json). 금지 목록은 [bannedWords]와 함께 막고, 허용 목록은 필드마다 [RoastSlang.maxPerField]개까지만 쓰게 한다.
 * 허용 유행어 안의 알파벳('L을 가져가')은 알파벳 검사에서 뺀다. 느낌표는 허용 목록에 필요한 말이 없어 그대로 막는다.
 */
data class RoastRules(
    @JsonProperty("residence_suffixes") val residenceSuffixes: List<String>,
    @JsonProperty("honorific_suffixes") val honorificSuffixes: List<String>,
    @JsonProperty("honorific_short_suffixes") val honorificShortSuffixes: List<String>,
    val particles: List<String>,
    @JsonProperty("suffix_exceptions") val suffixExceptions: List<String>,
    @JsonProperty("name_surnames") val nameSurnames: String,
    @JsonProperty("name_exceptions") val nameExceptions: List<String>,
    @JsonProperty("name_titles") val nameTitles: List<String>,
    @JsonProperty("unquotable_phrases") val unquotablePhrases: List<String>,
    @JsonProperty("quotable_max_length") val quotableMaxLength: Int,
    @JsonProperty("excluded_topics") val excludedTopics: List<String>,
    @JsonProperty("banned_words") val bannedWords: List<String>,
    @JsonProperty("number_patterns") val numberPatterns: List<String>,
    @JsonProperty("allowed_number_phrases") val allowedNumberPhrases: List<String>,
    @JsonProperty("coal_forbidden") val coalForbidden: List<String>,
    @JsonProperty("gift_forbidden") val giftForbidden: List<String>,
    @JsonProperty("tone_patterns") val tonePatterns: List<String>,
    @JsonProperty("title_banned_words") val titleBannedWords: List<String>,
    @JsonProperty("flip_words") val flipWords: List<String>,
    @JsonProperty("label_suffix") val labelSuffix: String,
    val slang: RoastSlang = RoastSlang.EMPTY,
) {
    private val particleGroup = alternation(particles)
    private val nameStems = NameStems(nameSurnames.toSet(), nameExceptions.toSet())
    private val exceptions = Regex("[가-힣]*(?:${alternation(suffixExceptions)})(?:$particleGroup)?")
    private val residences = tokenRegex(STEM_ONE_OR_MORE, residenceSuffixes)
    private val honorifics = tokenRegex(STEM_ANY, honorificSuffixes)
    private val shortHonorifics = tokenRegex(STEM_TWO_OR_MORE, honorificShortSuffixes)
    private val numbers = Regex(alternationOfPatterns(numberPatterns))
    private val allowedNumbers = Regex(alternation(allowedNumberPhrases))
    private val banned = Regex(alternationOfPatterns(listOf(alternation(bannedWords + slang.banned)) + slang.bannedPatterns))
    private val slangStems = Regex(alternation(slang.allowed.stems()))
    private val punchMarkers = Regex(alternation(slang.punchMarkers))
    private val coalWrong = Regex(alternation(coalForbidden))
    private val giftWrong = Regex(alternation(giftForbidden))
    private val tone = Regex(alternationOfPatterns(tonePatterns))
    private val titleBanned = Regex(alternation(titleBannedWords))
    private val flips = Regex(alternation(flipWords))

    /**
     * 작가 입력용으로 가린 본문. 호칭·부르는 말에서 떼어 낸 이름 줄기('현규형'의 '현규')는
     * [ScrubbedText.names]로 따로 내보내, 모든 줄을 다시 가리고([namePattern]) 차단 목록에 넣게 한다.
     */
    fun scrub(text: String): ScrubbedText {
        val masked = mutableListOf<String>()
        val names = mutableListOf<String>()
        val latinMasked = LATIN.replace(text) { word ->
            masked += word.value
            LATIN_LABEL
        }
        val scrubbed = HANGUL_TOKEN.replace(DIGITS.replace(latinMasked, DIGITS_LABEL)) { token -> maskToken(token.value, masked, names) }
        return ScrubbedText(scrubbed, masked, names)
    }

    /** 이름 줄기 [names]가 낱말 앞 경계에 나오면 뒤에 무엇이 붙든 (이름)으로 바꾸는 패턴. 줄기가 없으면 null이다. */
    fun namePattern(names: Collection<String>): MaskPattern? {
        if (names.isEmpty()) return null
        val words = names.distinct().sortedByDescending(String::length).joinToString(ALTERNATION) { name -> Regex.escape(name) }
        return MaskPattern(Regex("(?<![가-힣])(?:$words)"), NAME_LABEL)
    }

    /** 근거 카드 말풍선 표시에서 [names]를 ○○([QuoteText.NAME_MASK])로 바꾸는 패턴. 낱말 앞 경계에서만 맞춘다. 낱말이 없으면 null이다. */
    fun quotePattern(names: Collection<String>): MaskPattern? {
        val words = names.filter(String::isNotBlank).distinct().sortedByDescending(String::length)
        if (words.isEmpty()) return null
        return MaskPattern(Regex("(?<![가-힣])(?:${words.joinToString(ALTERNATION) { name -> Regex.escape(name) }})"), QuoteText.NAME_MASK)
    }

    /** 참여자 표시 이름 [speakers]로 만든 가릴 이름. 붙여 쓴 호칭·직함([honorificSuffixes], [honorificShortSuffixes], [nameTitles])은 떼고 이름 예외 목록으로 거른다. */
    fun participantNames(speakers: List<String>): ParticipantNames =
        ParticipantNames.of(speakers, honorificSuffixes + honorificShortSuffixes + nameTitles, nameExceptions.toSet())

    /** 출력 문장에서 사람 이름이나 사는 곳 이름으로 의심되는 낱말(호칭, 단지 접미사). 없으면 빈 목록이다. */
    fun suspectsIn(text: String): List<String> {
        val masked = mutableListOf<String>()
        HANGUL_TOKEN.replace(text) { token -> maskToken(token.value, masked, null) }
        return masked
    }

    /** 허용 표현("한 해", "한마디")을 뺀 뒤 처음 나온 숫자·수량·셈말 표현("세 번", "한 포대", "하나", "첫째"). 없으면 null이다. */
    fun numberIn(text: String): String? = numbers.find(allowedNumbers.replace(text, ""))?.value

    /** [slang]을 끼운 규칙. 앱이 뜰 때 rules.json과 slang.json을 합친다. */
    fun withSlang(slang: RoastSlang): RoastRules = copy(slang = slang)

    /** 허용 유행어를 [RoastSlang.maxPerField]개보다 많이 썼으면 쓴 유행어들, 아니면 null이다. 따옴표로 든 본인 입버릇('스불재')은 세지 않는다. */
    fun slangOverflowIn(text: String): String? {
        val found = slangStems.findAll(TextProblems.QUOTE.replace(text, "")).toList()
        return if (found.size > slang.maxPerField) found.joinToString(", ") { match -> match.value } else null
    }

    /** 처음 나온 금지어(대화 상대를 사람으로 가리키는 말, 지어낸 추억, 재심, 금지 유행어). 없으면 null이다. */
    fun bannedIn(text: String): String? = banned.find(text)?.value

    /** 판정과 어긋난 말. 석탄인데 선물을 받는다거나, 선물인데 석탄이라는 말이다. 없으면 null이다. */
    fun wrongDirectionIn(text: String, passed: Boolean): String? = (if (passed) giftWrong else coalWrong).find(text)?.value

    /** 처음 나온 금지 말투(옛말투 어미, 훈화 낱말, 유행어, 이모지, 느낌표, ㅋㅋ). 없으면 null이다. */
    fun toneIn(text: String): String? = tone.find(text)?.value

    /** 칭호에 처음 나온 칭호 금지어([titleBannedWords]). 없으면 null이다. */
    fun titleBannedIn(text: String): String? = titleBanned.find(text)?.value

    /** 처음 나온 칭찬 뒤집기 말([flipWords]). 없으면 null이다. */
    fun flipIn(text: String): String? = flips.find(text)?.value

    /**
     * 근거 카드 라벨이 상 이름 꼴인지. [MIN_AWARD_LABEL]자 이상이고 [labelSuffix]로 끝나야 한다.
     * 마지막 낱말이 일상·영상처럼 상으로 끝나는 흔한 낱말([COMMON_SANG_WORDS])이면 상 이름이 아니다.
     * 낱말 안에서 끝 두 글자만 견주지는 않는다(산타인정상은 정상이 아니다).
     */
    fun isAwardLabel(text: String): Boolean {
        val label = text.trim()
        return label.codePointCount(0, label.length) >= MIN_AWARD_LABEL && label.endsWith(labelSuffix) &&
            label.split(WHITESPACE).last() !in COMMON_SANG_WORDS
    }

    /**
     * 결론 마지막 한 줄 [text]가 밈 펀치인지. 허용 유행어([RoastSlang.allowed])나 구간 펀치 틀 표지([RoastSlang.punchMarkers])가 하나 이상 있어야 한다.
     * 조언·설명·덕담으로 끝나는 밋밋한 한 줄("'빨리'를 적게 써라.")을 막는다.
     */
    fun isPunch(text: String): Boolean = slangStems.containsMatchIn(text) || punchMarkers.containsMatchIn(text)

    /**
     * [text]에 쓴 허용 유행어 수. 덱 전체 상한([RoastSlang.maxPerDeck])을 셀 때 쓴다.
     * 따옴표로 든 본인 입버릇('스불재')은 작가 말투가 아니라 본인이 친 말이라 세지 않는다.
     */
    fun slangCountIn(text: String): Int = slangStems.findAll(TextProblems.QUOTE.replace(text, "")).count()

    /** 영문 낱말이 있는지. 허용 유행어 안의 알파벳은 보지 않는다. */
    fun hasLatin(text: String): Boolean = LATIN.containsMatchIn(slangStems.replace(text, ""))

    /**
     * [phraseKey] 입버릇의 표기 [display]를 작가에게 인용 후보로 넘겨도 되는지. 입버릇 칩 후보도 이 기준으로 고른다.
     * 금지어나 셈말을 품은 입버릇(킹받네, 몇 시 등)은 인용하면 검증에 떨어지므로 칩·대체 문구에도 싣지 않는다.
     */
    fun isQuotable(phraseKey: String, display: String): Boolean =
        phraseKey !in unquotablePhrases && display.codePointCount(0, display.length) <= quotableMaxLength &&
            !hasLatin(display) && !DIGITS.containsMatchIn(display) && bannedIn(display) == null && numberIn(display) == null

    /** 낱말 하나를 가린다. 이름 줄기를 모을 [names]가 없으면(출력 검사) 호칭 없이 부른 이름은 보지 않는다. */
    private fun maskToken(token: String, masked: MutableList<String>, names: MutableList<String>?): String {
        if (exceptions.matches(token)) return token
        val residence = residences.matchEntire(token)
        val honorific = if (residence == null) honorifics.matchEntire(token) ?: shortHonorifics.matchEntire(token) else null
        val match = residence ?: honorific
        if (match != null) {
            masked += match.groupValues[CORE]
            honorific?.groupValues?.get(STEM)?.takeIf(nameStems::accepts)?.let { stem -> names?.add(stem) }
            return (if (residence != null) RESIDENCE_LABEL else NAME_LABEL) + match.groupValues[PARTICLE]
        }
        if (names == null) return token
        val (stem, suffix) = nameStems.split(token) ?: return token
        if (nameStems.spreads(suffix)) {
            masked += stem
            names += stem
        }
        return NAME_LABEL + suffix
    }

    private fun tokenRegex(stem: String, suffixes: List<String>): Regex =
        Regex("(($stem)(?:${alternation(suffixes)}))((?:$particleGroup)?)")

    companion object {
        /** 상 이름 라벨의 최소 글자 수. */
        const val MIN_AWARD_LABEL = 3

        /** 상으로 끝나지만 상 이름이 아닌 흔한 낱말. 라벨이 이것으로 끝나면 떨어진다. */
        private val WHITESPACE = Regex("\\s+")
        private val COMMON_SANG_WORDS = setOf("일상", "밥상", "이상", "정상", "세상", "화상", "영상", "인상", "대상", "현상")
        private const val ALTERNATION = "|"
        private const val NOTHING = "(?!)"
        private const val CORE = 1
        private const val STEM = 2
        private const val PARTICLE = 3
        private const val STEM_ANY = "[가-힣]*"
        private const val STEM_ONE_OR_MORE = "[가-힣]+"
        private const val STEM_TWO_OR_MORE = "[가-힣]{2,}"
        private const val RESIDENCE_LABEL = "(집)"
        private const val NAME_LABEL = "(이름)"
        private const val LATIN_LABEL = "(영문)"
        private const val DIGITS_LABEL = "(숫자)"
        private val LATIN = Regex("[A-Za-zＡ-Ｚａ-ｚ]+")
        private val DIGITS = Regex("[0-9０-９]+(?:[,.:][0-9０-９]+)*")
        private val HANGUL_TOKEN = Regex("[가-힣]+")

        /** 목록을 글자 그대로 맞추는 정규식 선택지. 빈 목록이면 아무것도 맞지 않는다. */
        private fun alternation(words: List<String>): String =
            if (words.isEmpty()) NOTHING else words.joinToString(ALTERNATION) { word -> Regex.escape(word) }

        /** 정규식 목록을 하나로 잇는다. 빈 목록이면 아무것도 맞지 않는다. */
        private fun alternationOfPatterns(patterns: List<String>): String =
            if (patterns.isEmpty()) NOTHING else patterns.joinToString(ALTERNATION) { pattern -> "(?:$pattern)" }
    }
}
