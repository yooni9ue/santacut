package com.santacut.domain.roast.model

/** 근거 카드에 찍는 서버 숫자 칩. 숫자는 늘 서버가 센 값이다. 예: "'고마워'" / "0번". */
data class RoastChip(val label: String, val value: String) {
    /** 카드에 찍는 칩 글자. 이름과 값을 한 칸 띄워 잇는다(예: `'고마워' 0번`). */
    fun text(): String = "$label $value"
}
