package com.santacut.domain.roast.model

/**
 * 작가 응답 하나의 필드별 검증 결과. 필드 이름은 [RoastCheck.field]를 따른다.
 */
class RoastChecks(private val values: List<RoastCheck>) {
    fun toList(): List<RoastCheck> = values.toList()

    /** [field]의 검증이 있고 통과했는지. */
    fun passed(field: String): Boolean = values.any { check -> check.field == field && check.passed }

    /** 떨어진 필드 이름. 로그에는 이 이름과 수만 남긴다. */
    fun failedFields(): List<String> = values.filterNot(RoastCheck::passed).map(RoastCheck::field)

    /** [fields](title, pairs 같은 묶음 이름) 가운데 떨어진 필드의 사유. `pairs[2] roast: 숫자 표현(세 번)`처럼 필드 이름을 앞에 붙인다. */
    fun reasonsFor(fields: List<String>): List<String> = values
        .filter { check -> !check.passed && fields.any { field -> check.field == field || check.field.startsWith("$field[") } }
        .flatMap { check -> check.reasons.map { reason -> "${check.field} $reason" } }

    companion object {
        const val JSON_FIELD = "json"
        private const val UNREADABLE = "JSON으로 읽을 수 없다"

        /** JSON으로 읽을 수 없는 응답의 검증 결과. */
        fun unreadable(): RoastChecks = RoastChecks(listOf(RoastCheck(JSON_FIELD, listOf(UNREADABLE))))

        /** 응답 [output]의 모든 필드를 입력 [brief] 기준으로 검사한다. 짝은 [RoastSheet.MAX_PAIRS]개, 줄거리는 막 수만큼이어야 한다. */
        fun of(output: RoastOutput, brief: RoastBrief, rules: RoastRules): RoastChecks = RoastChecks(
            listOf(
                single(RoastSheet.TITLE, output.title, RoastTextKind.TITLE, brief, rules),
                single(RoastSheet.VERDICT, output.verdict, RoastTextKind.VERDICT, brief, rules),
                single(RoastSheet.VERDICT_MILD, output.verdictMild, RoastTextKind.VERDICT_MILD, brief, rules),
                single(RoastSheet.LETTER, output.letter, RoastTextKind.LETTER, brief, rules),
                count(RoastPairs.FIELD, output.pairs.size(), RoastSheet.MAX_PAIRS),
                count(RoastStoryLines.FIELD, output.story.size(), RoastSheet.ACTS),
            ) + output.pairs.checks(brief, rules) + output.story.checks(brief, rules),
        )

        private fun single(field: String, text: String, kind: RoastTextKind, brief: RoastBrief, rules: RoastRules): RoastCheck =
            RoastCheck(field, TextProblems.of(text, kind, brief, rules).toList())

        private fun count(field: String, actual: Int, expected: Int): RoastCheck =
            RoastCheck(field, listOfNotNull("${actual}개(${expected}개여야 한다)".takeUnless { actual == expected }))
    }
}
