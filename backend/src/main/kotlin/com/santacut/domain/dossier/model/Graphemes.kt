package com.santacut.domain.dossier.model

import java.text.BreakIterator

/**
 * 글 하나를 자소 묶음(사용자가 글자 하나로 보는 단위)으로 나눈 목록. 피부색·결합(ZWJ) 이모지를 한 개로 세려고 쓴다.
 */
class Graphemes(private val values: List<String>) {
    /**
     * 이모지 묶음. 이모지로 시작하고, [MAX_EMOJI_LENGTH]자 이하이고, 이모지·결합 문자(ZWJ, 이체 선택자, 키캡, 태그)로만 된 묶음이다.
     * 이모지 뒤에 결합 문자를 수십 개 붙인 묶음이나 글자가 섞인 묶음은 세지 않아 저장값에 엉뚱한 글이 들어가지 않게 한다.
     */
    fun emojis(): List<String> = values.filter { grapheme ->
        grapheme.length <= MAX_EMOJI_LENGTH && isEmoji(grapheme.codePointAt(0)) && grapheme.codePoints().allMatch(::isEmojiPart)
    }

    private fun isEmoji(codePoint: Int): Boolean = codePoint in PICTOGRAPHS || codePoint in SYMBOLS

    private fun isEmojiPart(codePoint: Int): Boolean =
        isEmoji(codePoint) || codePoint == ZERO_WIDTH_JOINER || codePoint == VARIATION_SELECTOR || codePoint == KEYCAP || codePoint in TAGS

    companion object {
        /** 이모지 묶음 하나의 최대 길이(UTF-16). 가족 이모지(👨‍👩‍👧‍👦)가 11자, 깃발 태그 묶음이 14자다. */
        const val MAX_EMOJI_LENGTH = 16
        private val PICTOGRAPHS = 0x1F000..0x1FAFF
        private val SYMBOLS = 0x2600..0x27BF
        private const val ZERO_WIDTH_JOINER = 0x200D
        private const val VARIATION_SELECTOR = 0xFE0F
        private const val KEYCAP = 0x20E3
        private val TAGS = 0xE0020..0xE007F

        fun of(text: String): Graphemes {
            val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
            val graphemes = mutableListOf<String>()
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                graphemes += text.substring(start, end)
                start = end
                end = iterator.next()
            }
            return Graphemes(graphemes)
        }
    }
}
