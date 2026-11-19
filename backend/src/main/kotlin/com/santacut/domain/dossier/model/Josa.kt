package com.santacut.domain.dossier.model

/**
 * 템플릿 슬롯 뒤에 붙이는 조사. `{slot:을}`처럼 어느 쪽 꼴로 적어도 값의 끝소리를 보고 맞는 꼴을 고른다.
 * 으로/로는 받침이 없거나 ㄹ 받침이면 '로'다.
 */
enum class Josa(private val afterFinal: String, private val afterVowel: String) {
    OBJECT("을", "를"),
    SUBJECT("이", "가"),
    TOPIC("은", "는"),
    WITH("과", "와"),
    DIRECTION("으로", "로"),
    ;

    /** [word] 뒤에 붙일 조사. */
    fun after(word: String): String {
        val sound = FinalSound.of(word)
        if (this == DIRECTION && sound == FinalSound.RIEUL) return afterVowel
        return if (sound == FinalSound.NONE) afterVowel else afterFinal
    }

    companion object {
        /** 템플릿에 적힌 조사 꼴. 모르는 꼴이면 null이다. */
        fun of(marker: String): Josa? = when (marker) {
            "을", "를" -> OBJECT
            "이", "가" -> SUBJECT
            "은", "는" -> TOPIC
            "과", "와" -> WITH
            "으로", "로" -> DIRECTION
            else -> null
        }
    }
}
