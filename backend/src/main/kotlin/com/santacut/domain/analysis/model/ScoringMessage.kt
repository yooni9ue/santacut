package com.santacut.domain.analysis.model

import java.time.LocalDateTime

/**
 * 채점 대화문을 만들 때 쓰는 메시지 하나. [localTime]은 한국 시각이다.
 *
 * @property speaker 화자 표시. 본인은 [SELF], 다른 참여자는 `상대`나 `상대1`, `상대2`다. 실제 이름은 담지 않는다.
 */
data class ScoringMessage(
    val localTime: LocalDateTime,
    val text: String,
    val speaker: String = SELF,
) {
    fun isSelf(): Boolean = speaker == SELF

    /** 사진·동영상·이모티콘·파일 첨부나 삭제 안내처럼 본문이 없는 메시지. 대화문에서 뺀다. */
    fun isPlaceholder(): Boolean = PLACEHOLDER.matches(text.trim())

    /**
     * 뜻을 잃지 않게 줄인 본문. 링크는 `(링크)`로 바꾸고, 줄바꿈과 겹친 공백을 공백 하나로 바꾸고, ㅋ·ㅎ는 세 개,
     * ㅠ·ㅜ와 문장 부호, 잇단 이모지는 두 개까지만 남긴다. 그래도 본인은 [MAX_TEXT_LENGTH]자, 상대는 [MAX_PARTNER_TEXT_LENGTH]자를
     * 넘으면 앞부분만 두고 `(긴 글 생략)`을 붙인다.
     */
    fun normalizedText(): String = shortened(
        text
            .replace(URL, LINK)
            .replace(WHITESPACE, SPACE)
            .replace(REPEATED_LAUGH, LAUGH_KEPT)
            .replace(REPEATED_CRY_OR_MARK, CRY_OR_MARK_KEPT)
            .replace(EMOJI_RUN) { run -> LEADING_TWO_EMOJI.find(run.value)?.value ?: run.value }
            .trim(),
    )

    /** 이모지 같은 서로게이트 쌍을 반으로 자르지 않게, 자른 끝이 앞쪽 반이면 그 글자를 뺀다. */
    private fun shortened(normalized: String): String {
        val maxLength = if (isSelf()) MAX_TEXT_LENGTH else MAX_PARTNER_TEXT_LENGTH
        if (normalized.length <= maxLength) return normalized
        val head = normalized.take(maxLength)
        return (if (head.last().isHighSurrogate()) head.dropLast(1) else head).trimEnd() + LONG_TEXT_SUFFIX
    }

    companion object {
        /** 본인 화자 표시. */
        const val SELF = "나"

        private const val SPACE = " "
        private val PLACEHOLDER = Regex("""사진( \d+장)?|동영상|이모티콘|파일(: .*)?|음성메시지|삭제된 메시지입니다\.|메시지가 삭제되었습니다\.""")
        private val WHITESPACE = Regex("""\s+""")
        private val REPEATED_LAUGH = Regex("""([ㅋㅎ])\1{3,}""")
        private val REPEATED_CRY_OR_MARK = Regex("""([ㅠㅜ!?.~])\1{2,}""")

        /** 잇단 ㅋ·ㅎ를 세 개로 줄인 치환 문자열(정규식 그룹 1을 세 번). */
        private const val LAUGH_KEPT = "\$1\$1\$1"

        /** 잇단 ㅠ·ㅜ와 문장 부호를 두 개로 줄인 치환 문자열(정규식 그룹 1을 두 번). */
        private const val CRY_OR_MARK_KEPT = "\$1\$1"
        private const val LINK = "(링크)"
        private val URL = Regex("""(?:https?://|www\.)\S+""", RegexOption.IGNORE_CASE)

        /** 본인 메시지 본문의 최대 글자 수. 넘으면 앞부분만 보낸다. */
        private const val MAX_TEXT_LENGTH = 300

        /** 상대 메시지 본문의 최대 글자 수. 상대 말은 점수에 들어가지 않고 문맥만 주므로 본인보다 짧게 둔다. */
        private const val MAX_PARTNER_TEXT_LENGTH = 150
        private const val LONG_TEXT_SUFFIX = " (긴 글 생략)"
        private const val EMOJI_BASE = """\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}"""
        private const val VARIATION_SELECTOR = """\x{FE0F}"""

        /**
         * 이모지로 시작해 이모지와 이체 선택자(U+FE0F)만 이어진 덩어리. 문자 클래스 하나의 반복이라 Java 정규식이 재귀 없이 맞춘다.
         * 그룹을 반복하면(`(?:이모지\x{FE0F}?)+`) 이모지 하나마다 스택을 써서 1,000개쯤 이어지면 StackOverflowError가 난다.
         */
        private val EMOJI_RUN = Regex("[$EMOJI_BASE][$EMOJI_BASE$VARIATION_SELECTOR]+")

        /** 덩어리 맨 앞 이모지 두 개(각각 이체 선택자 포함). 두 번만 반복하므로 스택을 거의 쓰지 않는다. */
        private val LEADING_TWO_EMOJI = Regex("^(?:[$EMOJI_BASE]$VARIATION_SELECTOR?){2}")
    }
}
