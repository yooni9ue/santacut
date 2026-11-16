package com.santacut.domain.analysis.model

/**
 * 본인 말에서 받은 배지 목록. [BadgeRule] 순서다. 상대 말은 세지 않고, 점수에 영향을 주지 않는다.
 */
class EarnedBadges(private val values: List<EarnedBadge>) {
    /** 표시 라벨 목록. */
    fun labels(): List<String> = values.map { badge -> badge.label() }

    /** 평가 행에 저장할 값(`THANKS_MACHINE:12,APOLOGY_SOLD_OUT:0`). 배지가 없으면 빈 문자열이다. */
    fun toColumn(): String = values.joinToString(COLUMN_SEPARATOR) { badge -> badge.toCell() }

    companion object {
        private const val COLUMN_SEPARATOR = ","

        /** 대화문 [turns]의 본인 줄에서 규칙마다 낱말을 세어 받은 배지만 남긴다. */
        fun countedIn(turns: TranscriptTurns): EarnedBadges = EarnedBadges(
            BadgeRule.entries.map { rule -> EarnedBadge(rule, turns.selfOccurrences(rule.keywords)) }
                .filter { badge -> badge.rule.isEarnedBy(badge.count) },
        )

        /** 평가 행에 저장한 값을 읽는다. 이 값이 생기기 전 평가(null)는 배지가 없다. */
        fun fromColumn(column: String?): EarnedBadges = EarnedBadges(
            column.orEmpty().split(COLUMN_SEPARATOR).mapNotNull { cell -> EarnedBadge.fromCell(cell) },
        )
    }
}
