package com.santacut.domain.dossier.model

/**
 * 극값 지표 한쪽(높음·낮음)의 문구. [big]은 큰 숫자 칸 하나의 템플릿이다.
 */
data class ExtremeSideCopy(
    val headline: Templates,
    val big: String,
    val lines: Templates,
    val memo: Templates,
) {
    /** 검사 위치 [path] 아래의 템플릿 묶음. */
    fun groups(path: String): Map<String, Templates> = mapOf(
        "$path.headline" to headline,
        "$path.big" to Templates(listOf(big)),
        "$path.lines" to lines,
        "$path.memo" to memo,
    )
}
