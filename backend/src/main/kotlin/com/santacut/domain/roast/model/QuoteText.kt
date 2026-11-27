package com.santacut.domain.roast.model

/**
 * 근거 카드 말풍선에 싣는 본인 말 표시. 서버가 원문에서 만든다.
 * 링크·전화번호·계좌번호처럼 보이는 숫자열을 지우고, 사람 이름과 사는 곳 이름을 ○○로 가린 뒤 [MAX_LENGTH]자에서 자르고 …을 붙인다.
 */
object QuoteText {
    /** 말풍선 글자 수 상한(…포함, 코드 포인트). */
    const val MAX_LENGTH = 20

    /** 가린 이름 자리 표시. */
    const val NAME_MASK = "○○"

    private const val ELLIPSIS = "…"
    private const val SPACE = " "
    private val LINK = Regex("(?i)(?:https?://|www\\.)\\S+")
    private val PHONE = Regex("\\d{2,4}[-. ]?\\d{3,4}[-. ]?\\d{4}")
    private val ACCOUNT = Regex("\\d[\\d-]{8,}\\d")
    private val SPACES = Regex("\\s+")
    private val NON_SYLLABLE = Regex("[^가-힣]")

    /** 근거 말풍선으로 쓰려면 필요한 한글 음절 수. ㅇㅇ·ㅋㅋ·ㄴㄴ 같은 자모 반응이나 넹·응 같은 단답은 버릇을 보여 주지 못한다. */
    const val MIN_SYLLABLES = 4

    /** 말풍선 표시 [text]가 근거로 쓸 만한지. 가린 이름(○○)과 자모·기호를 빼고 한글 음절이 [MIN_SYLLABLES]개 이상이어야 한다. */
    fun isSubstantial(text: String): Boolean = NON_SYLLABLE.replace(text, "").length >= MIN_SYLLABLES

    /** 원문 [original]을 말풍선 표시로 바꾼다. [names]는 ○○로 가릴 이름 패턴이고 없으면 null이다. 지우고 나서 비면 빈 문자열이다. */
    fun render(original: String, names: MaskPattern?): String {
        val stripped = PHONE.replace(ACCOUNT.replace(LINK.replace(original, ""), ""), "")
        val masked = names?.maskIn(stripped, mutableListOf()) ?: stripped
        return truncated(SPACES.replace(masked, SPACE).trim())
    }

    private fun truncated(text: String): String {
        if (text.codePointCount(0, text.length) <= MAX_LENGTH) return text
        return text.substring(0, text.offsetByCodePoints(0, MAX_LENGTH - ELLIPSIS.length)).trimEnd() + ELLIPSIS
    }
}
