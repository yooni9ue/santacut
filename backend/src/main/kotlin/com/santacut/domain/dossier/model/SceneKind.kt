package com.santacut.domain.dossier.model

/**
 * 장면 종류. 응답에는 소문자 이름([code])으로 나간다.
 */
enum class SceneKind {
    TEASER,
    HOOK,
    LEDGER,
    EXTREME,
    ACTS,
    DOSSIER,
    VERDICT,
    SUMMONS,
    ;

    fun code(): String = name.lowercase()
}
