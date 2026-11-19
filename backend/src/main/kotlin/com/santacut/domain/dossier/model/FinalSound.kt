package com.santacut.domain.dossier.model

/**
 * 낱말 끝소리(받침). 조사(을/를, 으로/로 등)를 고를 때 쓴다.
 * 끝의 따옴표·괄호 같은 문장 부호는 건너뛰고, 숫자는 읽는 소리(일·칠·팔은 ㄹ, 영·삼·육은 받침 있음)로, % 는 '퍼센트'로 본다.
 */
enum class FinalSound {
    NONE,
    RIEUL,
    OTHER,
    ;

    companion object {
        private const val SYLLABLE_START = 0xAC00
        private const val SYLLABLE_END = 0xD7A3
        private const val FINALS = 28
        private const val RIEUL_FINAL = 8
        private const val PERCENT = '%'
        private const val RIEUL_DIGITS = "178"
        private const val OTHER_DIGITS = "036"
        private const val RIEUL_LETTERS = "lL"
        private const val OTHER_LETTERS = "mnMN"

        fun of(word: String): FinalSound {
            val last = word.trimEnd { character -> !character.isLetterOrDigit() && character != PERCENT }.lastOrNull() ?: return NONE
            return when {
                last.code in SYLLABLE_START..SYLLABLE_END -> ofSyllable(last)
                last.isDigit() -> ofSpelled(last, RIEUL_DIGITS, OTHER_DIGITS)
                else -> ofSpelled(last, RIEUL_LETTERS, OTHER_LETTERS)
            }
        }

        private fun ofSyllable(syllable: Char): FinalSound = when ((syllable.code - SYLLABLE_START) % FINALS) {
            0 -> NONE
            RIEUL_FINAL -> RIEUL
            else -> OTHER
        }

        private fun ofSpelled(character: Char, rieul: String, other: String): FinalSound = when (character) {
            in rieul -> RIEUL
            in other -> OTHER
            else -> NONE
        }
    }
}
