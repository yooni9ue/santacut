package com.santacut.domain.dossier.model

import java.text.BreakIterator

/**
 * 글 하나를 자소 묶음(사용자가 글자 하나로 보는 단위)으로 나눈 목록. 피부색·결합 이모지를 한 개로 세려고 쓴다.
 */
class Graphemes(private val values: List<String>) {
    /** 이모지로 시작하는 묶음. */
    fun emojis(): List<String> = values.filter { grapheme -> isEmoji(grapheme.codePointAt(0)) }

    private fun isEmoji(codePoint: Int): Boolean = codePoint in PICTOGRAPHS || codePoint in SYMBOLS

    companion object {
        private val PICTOGRAPHS = 0x1F000..0x1FAFF
        private val SYMBOLS = 0x2600..0x27BF

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
