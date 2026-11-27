package com.santacut.domain.roast.model

/**
 * 작가 호출을 거듭하며 모은, 검증을 통과한 덱 문장. 묶음(판정 한 줄, 근거 카드 전체, 결론) 단위로 처음 통과한 것을 지키고 빠진 묶음만 다음 응답에서 채운다.
 * 근거 카드는 kind별 개수까지 모두 통과해야 한 번에 받는다. 묶음을 더할 때 덱 전체 허용 유행어가 상한([RoastSlang.maxPerDeck])을 넘으면 그 묶음은 받지 않는다.
 */
class RoastSheet(
    private val verdictLine: String?,
    private val evidence: EvidenceDrafts?,
    private val conclusion: ConclusionDraft?,
) {
    /** 응답 [review]에서 통과한 묶음으로 빈칸을 채운다. 읽을 수 없는 응답이면 그대로다. */
    fun absorb(review: RoastReview, rules: RoastRules): RoastSheet {
        val output = review.output ?: return this
        val checks = review.checks
        val withVerdict = RoastSheet(verdictLine ?: output.verdictLine.takeIf { checks.passed(RoastChecks.VERDICT_LINE_FIELD) }, evidence, conclusion)
            .orIfOverflow(this, rules)
        val withEvidence = RoastSheet(withVerdict.verdictLine, evidence ?: output.evidence.takeIf { checks.passedGroup(EvidenceDrafts.FIELD) }, conclusion)
            .orIfOverflow(withVerdict, rules)
        return RoastSheet(withEvidence.verdictLine, withEvidence.evidence, conclusion ?: output.conclusion.takeIf { checks.passed(ConclusionDraft.FIELD) })
            .orIfOverflow(withEvidence, rules)
    }

    /** 아직 채우지 못한 묶음 이름(다시 쓰기 대상). */
    fun missingFields(): List<String> = listOfNotNull(
        RoastChecks.VERDICT_LINE_FIELD.takeIf { verdictLine == null },
        EvidenceDrafts.FIELD.takeIf { evidence == null },
        ConclusionDraft.FIELD.takeIf { conclusion == null },
    )

    fun isComplete(): Boolean = missingFields().isEmpty()

    /** 빠진 묶음과 직전 응답 [review]의 사유로 다시 쓰기 요청을 만든다. */
    fun rewriteFor(review: RoastReview): RoastRewrite {
        val missing = missingFields()
        return RoastRewrite(missing, review.checks.reasonsFor(missing + RoastChecks.JSON_FIELD + RoastChecks.DECK_FIELD))
    }

    /**
     * 빈칸을 서버 대체 문장으로 채워 저장할 내용을 만든다. 판정 한 줄과 결론은 [brief] 판정 구간의 고정 문장,
     * 근거 카드는 칩 후보로 만든 통계 카드다. 작가 문장과 합쳐 덱 전체 유행어가 상한을 넘으면 유행어 없는 고정 문장을 쓴다.
     */
    fun toContent(brief: RoastBrief, fallback: RoastFallback, rules: RoastRules): RoastContent {
        val grade = brief.verdict.grade()
        val band = fallback.bandOf(grade)
        val cards = evidence?.toEvidences(brief) ?: brief.fallbackEvidence(fallback)
        val fallbackSlang = (if (verdictLine == null) rules.slangCountIn(band.verdictLine) else 0) +
            (if (conclusion == null) rules.slangCountIn(band.finalLine) else 0)
        val plain = slangCount(rules) + fallbackSlang > rules.slang.maxPerDeck
        return RoastContent(
            brief.verdict.key(),
            RoastText.of(verdictLine, band.verdictLine(plain)),
            cards,
            conclusion?.toConclusion(brief) ?: fallback.conclusionOf(grade, cards.hero(), plain),
        )
    }

    private fun slangCount(rules: RoastRules): Int =
        (verdictLine?.let(rules::slangCountIn) ?: 0) + (evidence?.slangCount(rules) ?: 0) + (conclusion?.slangCount(rules) ?: 0)

    /** 덱 전체 유행어가 상한을 넘으면 [previous]를 돌려준다. */
    private fun orIfOverflow(previous: RoastSheet, rules: RoastRules): RoastSheet =
        if (slangCount(rules) > rules.slang.maxPerDeck) previous else this

    companion object {
        /** 아무것도 통과하지 못한 처음 상태. 작가를 부르지 않으면 이대로 대체 문장만 쓴다. */
        val EMPTY = RoastSheet(null, null, null)
    }
}
