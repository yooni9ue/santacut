package com.santacut.domain.dossier.model

/**
 * 극값 지표 하나의 문구. 백분위가 높은 쪽([high])과 낮은 쪽([low])을 따로 둔다.
 *
 * @property speaker 이 장의 화자 key(copy.json의 `speakers`에 있어야 한다)
 */
data class ExtremeCopy(
    val speaker: String,
    val high: ExtremeSideCopy,
    val low: ExtremeSideCopy,
) {
    fun side(isHigh: Boolean): ExtremeSideCopy = if (isHigh) high else low

    fun groups(path: String): Map<String, Templates> = high.groups("$path.high") + low.groups("$path.low")
}
