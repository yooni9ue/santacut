package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.dossier.model.TextForms

/**
 * 팩폭 작가의 입력 안전장치와 출력 검증 규칙(roast/rules.json). 목록은 앱이 뜰 때 정규식으로 한 번 바꿔 둔다.
 *
 * 입력: 돈(금액, 빚, 계좌 등)·정치·종교 낱말이 든 줄은 통째로 빼고([sensitiveForms], [sensitivePatterns]), 영문 낱말은 (영문),
 * 숫자열은 (숫자), 사람 이름은 호칭([honorificSuffixes] 등) 패턴과 호칭 없이 부른 이름([NameStems]) 규칙으로 (이름), 아파트·빌라 같은
 * 사는 곳 이름([residenceSuffixes])은 (집)으로 바꾼다. 지명·가게·행사 이름은 가리지 않는다(사람 이름만 가린다는 결정).
 * 접미사 패턴은 오탐이 잦은 낱말([suffixExceptions])을 건너뛴다.
 * 출력: 숫자 표현([numberPatterns], 허용 [allowedNumberPhrases]), 금지어([bannedWords]), 판정 방향([coalForbidden], [giftForbidden]),
 * 담백 판결의 석탄·탈락([mildForbidden]), "근데"로 여는 뒤집기([flipForbiddenStarts])를 막는다.
 *
 * @property residenceSuffixes 줄기 한 글자 이상에 붙으면 사는 곳(주소에 가까운 단지) 이름으로 보는 접미사(아파트, 빌라 등)
 * @property honorificSuffixes 붙으면 사람 이름으로 보는 호칭(누나, 언니 등). 호칭만 있어도 가린다.
 * @property honorificShortSuffixes 줄기 두 글자 이상에 붙으면 사람 이름으로 보는 한 글자 호칭(형, 님, 씨)
 * @property particles 낱말 뒤에 붙어도 같은 낱말로 보는 조사
 * @property nameSurnames 세 글자 이름 줄기로 볼 첫 글자(흔한 성)
 * @property nameExceptions 이름 줄기로 보지 않는 흔한 말(친구, 얘들, 사람 등)
 * @property unquotablePhrases quotable로 넘기지 않는 입버릇 key(비속어, 사람 부르는 말). 조서 통계에는 그대로 남는다.
 * @property quotableMaxLength quotable로 넘기는 입버릇 표기의 최대 글자 수
 * @property excludedTopics 화제 후보로 넘기지 않는 조서 주제 key(직장과 엮인 출퇴근 등)
 */
data class RoastRules(
    @JsonProperty("sensitive_forms") val sensitiveForms: TextForms,
    @JsonProperty("sensitive_patterns") val sensitivePatterns: List<String>,
    @JsonProperty("residence_suffixes") val residenceSuffixes: List<String>,
    @JsonProperty("honorific_suffixes") val honorificSuffixes: List<String>,
    @JsonProperty("honorific_short_suffixes") val honorificShortSuffixes: List<String>,
    val particles: List<String>,
    @JsonProperty("suffix_exceptions") val suffixExceptions: List<String>,
    @JsonProperty("name_surnames") val nameSurnames: String,
    @JsonProperty("name_exceptions") val nameExceptions: List<String>,
    @JsonProperty("unquotable_phrases") val unquotablePhrases: List<String>,
    @JsonProperty("quotable_max_length") val quotableMaxLength: Int,
    @JsonProperty("excluded_topics") val excludedTopics: List<String>,
    @JsonProperty("banned_words") val bannedWords: List<String>,
    @JsonProperty("number_patterns") val numberPatterns: List<String>,
    @JsonProperty("allowed_number_phrases") val allowedNumberPhrases: List<String>,
    @JsonProperty("coal_forbidden") val coalForbidden: List<String>,
    @JsonProperty("gift_forbidden") val giftForbidden: List<String>,
    @JsonProperty("mild_forbidden") val mildForbidden: List<String>,
    @JsonProperty("flip_forbidden_starts") val flipForbiddenStarts: List<String>,
) {
    private val particleGroup = alternation(particles)
    private val sensitive = Regex(alternationOfPatterns(sensitivePatterns))
    private val nameStems = NameStems(nameSurnames.toSet(), nameExceptions.toSet())
    private val exceptions = Regex("[가-힣]*(?:${alternation(suffixExceptions)})(?:$particleGroup)?")
    private val residences = tokenRegex(STEM_ONE_OR_MORE, residenceSuffixes)
    private val honorifics = tokenRegex(STEM_ANY, honorificSuffixes)
    private val shortHonorifics = tokenRegex(STEM_TWO_OR_MORE, honorificShortSuffixes)
    private val numbers = Regex(alternationOfPatterns(numberPatterns))
    private val allowedNumbers = Regex(alternation(allowedNumberPhrases))
    private val banned = Regex(alternation(bannedWords))
    private val coalWrong = Regex(alternation(coalForbidden))
    private val giftWrong = Regex(alternation(giftForbidden))
    private val mildWrong = Regex(alternation(mildForbidden))
    private val flipWrongStart = Regex("^\\s*(?:${alternation(flipForbiddenStarts)})")

    /** 통째로 뺄 줄인지(돈·정치·종교 낱말, 금액). 직장·건강·연애·가족 줄은 보낸다는 결정에 따라 빼지 않는다. */
    fun isSensitive(text: String): Boolean = sensitiveForms.appearsIn(text) || sensitive.containsMatchIn(text)

    /**
     * 작가 입력용으로 가린 본문. 통째로 뺄 줄이면 null이다. 호칭·부르는 말에서 떼어 낸 이름 줄기('현규형'의 '현규')는
     * [ScrubbedText.names]로 따로 내보내, 모든 줄을 다시 가리고([namePattern]) 차단 목록에 넣게 한다.
     */
    fun scrub(text: String): ScrubbedText? {
        if (isSensitive(text)) return null
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

    /** 출력 문장에서 사람 이름이나 사는 곳 이름으로 의심되는 낱말(호칭, 단지 접미사). 없으면 빈 목록이다. */
    fun suspectsIn(text: String): List<String> {
        val masked = mutableListOf<String>()
        HANGUL_TOKEN.replace(text) { token -> maskToken(token.value, masked, null) }
        return masked
    }

    /** 허용 표현("한 해", "한마디")을 뺀 뒤 처음 나온 숫자·수량 표현. 없으면 null이다. */
    fun numberIn(text: String): String? = numbers.find(allowedNumbers.replace(text, ""))?.value

    /** 처음 나온 금지어(남 언급, 돈·직장, 지어낸 추억, 재심). 없으면 null이다. */
    fun bannedIn(text: String): String? = banned.find(text)?.value

    /** 판정과 어긋난 말. 석탄인데 선물을 받는다거나, 선물인데 석탄이라는 말이다. 없으면 null이다. */
    fun wrongDirectionIn(text: String, passed: Boolean): String? = (if (passed) giftWrong else coalWrong).find(text)?.value

    /** 담백 판결에 들어가면 안 되는 말(석탄, 탈락). 없으면 null이다. */
    fun mildWrongIn(text: String): String? = mildWrong.find(text)?.value

    fun startsFlipWrongly(text: String): Boolean = flipWrongStart.containsMatchIn(text)

    /** 영문 낱말이 있는지. */
    fun hasLatin(text: String): Boolean = LATIN.containsMatchIn(text)

    /** [phraseKey] 입버릇의 표기 [display]를 작가에게 인용 후보로 넘겨도 되는지. */
    fun isQuotable(phraseKey: String, display: String): Boolean =
        phraseKey !in unquotablePhrases && display.codePointCount(0, display.length) <= quotableMaxLength &&
            !hasLatin(display) && !DIGITS.containsMatchIn(display)

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
        masked += stem
        names += stem
        return NAME_LABEL + suffix
    }

    private fun tokenRegex(stem: String, suffixes: List<String>): Regex =
        Regex("(($stem)(?:${alternation(suffixes)}))((?:$particleGroup)?)")

    companion object {
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
