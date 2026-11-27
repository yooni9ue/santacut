package com.santacut.domain.roast.model

/** 근거 카드에 찍는 서버 숫자 칩. 숫자는 늘 서버가 센 값이다. 예: "'고마워'" / "0번". */
data class RoastChip(val label: String, val value: String) {
    /** 카드에 찍는 칩 글자. 이름과 값을 한 칸 띄워 잇는다(예: `'고마워' 0번`). */
    fun text(): String = "$label $value"

    /** 칩 이름에 작은따옴표로 든 말(입버릇 칩 `'수고'`의 수고). 없으면 null이다. */
    fun quotedPhrase(): String? = QUOTED.find(label)?.groupValues?.get(PHRASE_GROUP)

    companion object {
        private val QUOTED = Regex("'([^']+)'")
        private const val PHRASE_GROUP = 1
    }
}
