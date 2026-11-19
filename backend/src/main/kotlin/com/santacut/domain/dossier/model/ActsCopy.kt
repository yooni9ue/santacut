package com.santacut.domain.dossier.model

/**
 * 「올해의 너 3막」 문구(copy.json의 `acts`). [empty]는 화제가 없는 막의 줄이다.
 */
data class ActsCopy(
    val headline: Templates,
    val line: Templates,
    val empty: Templates,
)
