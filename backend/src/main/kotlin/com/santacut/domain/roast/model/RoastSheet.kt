package com.santacut.domain.roast.model

/**
 * 작가 호출을 거듭하며 모은, 검증을 통과한 덱 문장. 판정 한 줄과 결론은 묶음 단위로 처음 통과한 것을 지키고 빠진 묶음만 다음 응답에서 채운다.
 * 근거 카드는 카드 단위로 지킨다. 통과한 카드는 남기고 kind별 슬롯이 다 찰 때까지 다음 응답에서 떨어진 자리만 채운다.
 * 무언가를 더할 때 덱 전체 허용 유행어가 상한([RoastSlang.maxPerDeck])을 넘으면 그것은 받지 않는다.
 *
 * @property evidence 지금까지 통과한 근거 카드
 * @property evidenceFilled 근거 카드가 kind별 슬롯 수만큼 다 찼는지
 */
class RoastSheet(
    private val verdictLine: String?,
    private val evidence: EvidenceDrafts,
    private val evidenceFilled: Boolean,
    private val conclusion: ConclusionDraft?,
) {
    /** 응답 [review]에서 통과한 묶음과 근거 카드로 빈칸을 채운다. 근거 카드 슬롯 수는 [brief]를 따른다. 읽을 수 없는 응답이면 그대로다. */
    fun absorb(review: RoastReview, brief: RoastBrief, rules: RoastRules): RoastSheet {
        val output = review.output ?: return this
        val checks = review.checks
        val withVerdict = copy(verdictLine = verdictLine ?: output.verdictLine.takeIf { checks.passed(RoastChecks.VERDICT_LINE_FIELD) })
            .orIfOverflow(this, rules)
        val withEvidence = withVerdict.withEvidenceFrom(output.evidence, checks, brief.slots).orIfOverflow(withVerdict, rules)
        return withEvidence.copy(conclusion = conclusion ?: output.conclusion.takeIf { checks.passed(ConclusionDraft.FIELD) })
            .orIfOverflow(withEvidence, rules)
    }

    /** 아직 채우지 못한 묶음 이름(다시 쓰기 대상). 근거 카드는 한 장이라도 비면 빈 묶음이다. */
    fun missingFields(): List<String> = listOfNotNull(
        RoastChecks.VERDICT_LINE_FIELD.takeIf { verdictLine == null },
        EvidenceDrafts.FIELD.takeUnless { evidenceFilled },
        ConclusionDraft.FIELD.takeIf { conclusion == null },
    )

    fun isComplete(): Boolean = missingFields().isEmpty()

    /** 빠진 묶음과 직전 응답 [review]의 사유로 다시 쓰기 요청을 만든다. */
    fun rewriteFor(review: RoastReview): RoastRewrite {
        val missing = missingFields()
        return RoastRewrite(missing, review.checks.reasonsFor(missing + RoastChecks.JSON_FIELD + RoastChecks.DECK_FIELD))
    }

    /**
     * 빈칸을 서버 대체 문장으로 채워 저장할 내용을 만든다. 판정 한 줄과 결론은 [brief] 판정 구간의 고정 문장이다.
     * 근거 카드는 통과한 작가 카드를 쓰고, 빈 슬롯은 칩 후보로 만든 통계 카드(입버릇·튀는 지표 칩)로 채운다.
     * 슬롯이 다 찼어도 근거 카드가 [RoastEvidences.MIN_CARDS]장 아래면 같은 통계 카드로 그 수까지 채운다. 항목 점수 카드는 덱에 실을 때([RoastContent.forDeck])만 쓴다.
     * 작가 문장과 합쳐 덱 전체 유행어가 상한을 넘으면 유행어 없는 고정 문장을 쓴다.
     */
    fun toContent(brief: RoastBrief, fallback: RoastFallback, rules: RoastRules): RoastContent {
        val grade = brief.verdict.grade()
        val band = fallback.bandOf(grade)
        val kept = evidence.toEvidences(brief)
        val stats = brief.fallbackEvidence(fallback, kept)
        val cards = kept.plus(stats, if (evidenceFilled) RoastEvidences.MIN_CARDS - kept.size() else stats.size())
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

    /** 응답 근거 카드 [drafts] 가운데 통과한 카드로 빈 슬롯을 채운 시트. 이미 다 찼으면 그대로다. */
    private fun withEvidenceFrom(drafts: EvidenceDrafts, checks: RoastChecks, slots: RoastSlots): RoastSheet {
        if (evidenceFilled) return this
        val merged = evidence.absorbed(drafts, checks, slots)
        return copy(evidence = merged, evidenceFilled = merged.fills(slots))
    }

    private fun copy(
        verdictLine: String? = this.verdictLine,
        evidence: EvidenceDrafts = this.evidence,
        evidenceFilled: Boolean = this.evidenceFilled,
        conclusion: ConclusionDraft? = this.conclusion,
    ): RoastSheet = RoastSheet(verdictLine, evidence, evidenceFilled, conclusion)

    private fun slangCount(rules: RoastRules): Int =
        (verdictLine?.let(rules::slangCountIn) ?: 0) + evidence.slangCount(rules) + (conclusion?.slangCount(rules) ?: 0)

    /** 덱 전체 유행어가 상한을 넘으면 [previous]를 돌려준다. */
    private fun orIfOverflow(previous: RoastSheet, rules: RoastRules): RoastSheet =
        if (slangCount(rules) > rules.slang.maxPerDeck) previous else this

    companion object {
        /** 아무것도 통과하지 못한 처음 상태. 작가를 부르지 않으면 이대로 대체 문장만 쓴다. */
        val EMPTY = RoastSheet(null, EvidenceDrafts.EMPTY, false, null)
    }
}
