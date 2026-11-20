package com.santacut.domain.roast.model

/**
 * 호칭 없이 부르거나 가리킨 사람 이름을 찾는 규칙. '동준아', '민수야', '지은이랑', '현규한테'처럼 이름 줄기(두세 글자) 뒤에
 * 부르는 말이나 조사가 붙은 낱말을 이름으로 본다. 입력에만 쓰고 출력 검사에는 쓰지 않는다(흔한 말 오탐이 문장을 버리게 하지 않으려는 것).
 *
 * - 받침 있는 줄기: '아'(끝 모음이 ㅏ·ㅑ·ㅗ·ㅘ·ㅛ면 '좋아', '맞아' 같은 용언이라 뺀다), '이랑', '이가', '이는', '이한테'
 * - 받침 없는 줄기: '야'(끝 글자가 '먹어야', '할거야'처럼 용언 어미면 뺀다)
 * - 받침과 상관없이: '한테'
 * - 세 글자 줄기는 첫 글자가 흔한 성([surnames])일 때만 본다. 흔한 말([exceptions])은 줄기로 보지 않는다.
 * 부르는 말('아', '야')로 찾은 줄기만 다른 줄과 출력 차단 목록으로 퍼뜨린다([spreads]). 조사('이랑', '이는', '한테')로 찾은 줄기는
 * '치킨이랑', '움직이는'처럼 흔한 말일 때가 많아 그 자리에서만 가린다.
 */
class NameStems(private val surnames: Set<Char>, private val exceptions: Set<String>) {
    /** [token]이 이름 줄기 + 부르는 말·조사면 (줄기, 붙은 말), 아니면 null이다. */
    fun split(token: String): Pair<String, String>? {
        val suffix = SUFFIXES.firstOrNull { suffix -> token.endsWith(suffix) && token.length - suffix.length in STEM_LENGTHS } ?: return null
        val stem = token.dropLast(suffix.length)
        return (stem to suffix).takeIf { accepts(stem) && fits(stem.last(), suffix) }
    }

    /** [suffix]로 찾은 줄기를 모든 줄과 출력 차단 목록으로 퍼뜨릴지. 부르는 말('아', '야')일 때만 참이다. */
    fun spreads(suffix: String): Boolean = suffix == VOCATIVE_AFTER_FINAL || suffix == VOCATIVE_AFTER_VOWEL

    /** 호칭 패턴('현규형', '민지님')에서 떼어 낸 줄기 [stem]을 이름으로 다시 가려도 되는지. */
    fun accepts(stem: String): Boolean =
        stem.length in STEM_LENGTHS && stem.all { character -> character in HANGUL } && stem !in exceptions &&
            (stem.length < THREE || stem.first() in surnames)

    private fun fits(last: Char, suffix: String): Boolean {
        val code = last - HANGUL.first
        val hasFinal = code % FINALS != 0
        val vowel = code / FINALS % VOWELS
        return when (suffix) {
            VOCATIVE_AFTER_FINAL -> hasFinal && vowel !in BRIGHT_VOWELS
            VOCATIVE_AFTER_VOWEL -> !hasFinal && last !in VERB_ENDINGS
            TO -> true
            else -> hasFinal
        }
    }

    companion object {
        private val HANGUL = '가'..'힣'
        private val STEM_LENGTHS = 2..3
        private const val THREE = 3
        private const val FINALS = 28
        private const val VOWELS = 21
        private const val VOCATIVE_AFTER_FINAL = "아"
        private const val VOCATIVE_AFTER_VOWEL = "야"
        private const val TO = "한테"

        /** 긴 것부터 맞춘다('이한테'가 '한테'보다 먼저). */
        private val SUFFIXES = listOf("이한테", "이랑", "이가", "이는", TO, VOCATIVE_AFTER_FINAL, VOCATIVE_AFTER_VOWEL)

        /** ㅏ, ㅑ, ㅗ, ㅘ, ㅛ(중성 순번). 이 모음 뒤 '-아'는 용언 활용이다. */
        private val BRIGHT_VOWELS = setOf(0, 2, 8, 9, 12)

        /** 이 글자로 끝난 말 뒤 '-야'는 '해야', '먹어야', '할거야' 같은 용언 어미나 '말이야', '집이야' 같은 서술격 조사다. */
        private val VERB_ENDINGS = "아어여와워해봐줘져려내돼써쳐펴껴겨셔러버뻐래거꺼게가대데네다라까고요죠이".toSet()
    }
}
