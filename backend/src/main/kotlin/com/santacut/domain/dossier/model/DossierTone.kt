package com.santacut.domain.dossier.model

/**
 * 조서 문구의 톤. 위기·무거운 표현이 기준을 넘으면 담백([MILD])으로 죄목을 빼고 놀림 문구를 끈다.
 */
enum class DossierTone {
    NORMAL,
    MILD,
    ;

    fun isMild(): Boolean = this == MILD

    /** 응답에 쓰는 소문자 이름(normal, mild). */
    fun code(): String = name.lowercase()
}
