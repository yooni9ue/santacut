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

    /** [fields](verdict_line, evidence 같은 묶음 이름) 가운데 떨어진 필드의 사유. `evidence[2] line: 숫자 표현(세 번)`처럼 필드 이름을 앞에 붙인다. */
    fun reasonsFor(fields: List<String>): List<String> = values
        .filter { check -> !check.passed && fields.any { field -> isIn(check, field) } }
        .flatMap { check -> check.reasons.map { reason -> "${check.field} $reason" } }

    private fun isIn(check: RoastCheck, field: String): Boolean = check.field == field || check.field.startsWith("$field[")

    companion object {
        const val JSON_FIELD = "json"
        const val DECK_FIELD = "deck"
        const val VERDICT_LINE_FIELD = "verdict_line"
        private const val UNREADABLE = "JSON으로 읽을 수 없다"

        /** JSON으로 읽을 수 없는 응답의 검증 결과. */
        fun unreadable(): RoastChecks = RoastChecks(listOf(RoastCheck(JSON_FIELD, listOf(UNREADABLE))))

        /**
         * 응답 [output]의 모든 필드를 입력 [brief] 기준으로 검사한다. 근거 카드는 kind별 개수가 서버 슬롯 수와 같아야 하고,
         * 덱 전체 허용 유행어는 [RoastSlang.maxPerDeck]개까지다(필드 deck). 결론은 판정 한 줄·근거 카드와 함께 본다(펀치 되풀이, 다섯 장 덱의 대표 말 되풀이),
         * 근거 카드 라벨은 판정 한 줄을 옮기면 떨어진다. 대표 사실 칩은 앞 응답에서 받아 둔 근거 카드 [kept]에 있어도 쓴 것으로 본다.
         */
        fun of(output: RoastOutput, brief: RoastBrief, rules: RoastRules, kept: EvidenceDrafts = EvidenceDrafts.EMPTY): RoastChecks = RoastChecks(
            listOf(
                RoastCheck(
                    VERDICT_LINE_FIELD,
                    TextProblems.of(output.verdictLine, RoastTextKind.VERDICT_LINE, brief, rules).toList() +
                        TextProblems.copiedExample(output.verdictLine, brief).toList(),
                ),
                output.evidence.countCheck(brief),
                output.conclusion.check(brief, rules, output.verdictLine, output.evidence, kept),
                deckCheck(output, rules),
            ) + output.evidence.checks(brief, rules, output.verdictLine),
        )

        private fun deckCheck(output: RoastOutput, rules: RoastRules): RoastCheck {
            val count = output.slangCount(rules)
            val limit = rules.slang.maxPerDeck
            return RoastCheck(DECK_FIELD, listOfNotNull("유행어는 덱 전체에 ${limit}개까지(${count}개)".takeIf { count > limit }))
        }
    }
}
