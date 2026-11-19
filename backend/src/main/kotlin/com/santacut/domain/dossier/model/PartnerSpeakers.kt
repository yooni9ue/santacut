package com.santacut.domain.dossier.model

/**
 * 대화에 나온 상대 화자 표시(`상대`, `상대1` 등). 이름은 담지 않는다.
 */
class PartnerSpeakers(private val values: MutableSet<String> = HashSet()) {
    fun add(speaker: String) {
        values += speaker
    }

    /** 상대 화자가 한 명인 1:1 대화인지. */
    fun isOneOnOne(): Boolean = values.size == 1

    /** 상대 화자가 둘 이상인 단체방인지. */
    fun isGroup(): Boolean = values.size > 1
}
