package com.santacut.domain.dossier.model

/**
 * 「올해의 너 3막」의 막. 4개월씩 나눈다. [label]은 화면에 그대로 쓰는 서버 표기다.
 */
enum class DossierAct(val months: IntRange, val label: String) {
    FIRST(1..4, "1막(1~4월)"),
    SECOND(5..8, "2막(5~8월)"),
    THIRD(9..12, "3막(9~12월)"),
    ;

    companion object {
        /** [month](1~12)가 든 막. */
        fun of(month: Int): DossierAct = entries[(month - 1) / MONTHS_PER_ACT]

        private const val MONTHS_PER_ACT = 4
    }
}
