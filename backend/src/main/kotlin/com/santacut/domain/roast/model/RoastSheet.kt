package com.santacut.domain.roast.model

/**
 * 작가 호출을 거듭하며 모은, 검증을 통과한 덱 문장. 판정 한 줄과 결론은 묶음 단위로 처음 통과한 것을 지키고 빠진 묶음만 다음 응답에서 채운다.
 * 근거 카드는 카드 단위로 지킨다. 통과한 카드는 남기고 kind별 슬롯이 다 찰 때까지 다음 응답에서 떨어진 자리만 채운다.
 * 무언가를 더할 때 덱 전체 허용 유행어가 상한([RoastSlang.maxPerDeck])을 넘거나, 판정 한 줄과 결론 마지막 한 줄이 같은 펀치 표지(유행어·펀치 틀)를 쓰게 되면
 * 그것은 받지 않는다. 판정 한 줄과 결론이 다른 응답에서 와도 펀치가 되풀이되지 않게 한다.
 * 검증은 통과했지만 이렇게 받지 않은 묶음·카드는 사유([rejections])를 남겨 다시 쓰기 요청에 싣는다. 작가가 같은 실수를 되풀이하지 않게 한다.
 *
 * @property evidence 지금까지 통과한 근거 카드
 * @property evidenceFilled 근거 카드가 kind별 슬롯 수만큼 다 찼는지
 * @property rejections 마지막으로 받은 응답에서 검증은 통과했지만 받아 둔 문장과 겹쳐 받지 않은 사유
 */
class RoastSheet(
    private val verdictLine: String?,
    private val evidence: EvidenceDrafts,
    private val evidenceFilled: Boolean,
    private val conclusion: ConclusionDraft?,
    private val rejections: List<String> = emptyList(),
) {
    /** 응답 [review]에서 통과한 묶음과 근거 카드로 빈칸을 채운다. 근거 카드 슬롯 수는 [brief]를 따른다. 읽을 수 없는 응답이면 그대로다. */
    fun absorb(review: RoastReview, brief: RoastBrief, rules: RoastRules): RoastSheet {
        val output = review.output ?: return this
        val checks = review.checks
        val start = copy(rejections = emptyList())
        val newVerdict = output.verdictLine.takeIf { verdictLine == null && checks.passed(RoastChecks.VERDICT_LINE_FIELD) }
        val withVerdict = start.copy(verdictLine = verdictLine ?: newVerdict).orIfRejected(start, rules, RoastChecks.VERDICT_LINE_FIELD)
        val withEvidence = withVerdict.withEvidenceFrom(output.evidence, checks, brief, rules).orIfRejected(withVerdict, rules, EvidenceDrafts.FIELD)
        val newConclusion = output.conclusion.takeIf { conclusion == null && checks.passed(ConclusionDraft.FIELD) }
        return withEvidence.copy(conclusion = conclusion ?: newConclusion).orIfRejected(withEvidence, rules, ConclusionDraft.FIELD)
    }

    /** 아직 채우지 못한 묶음 이름(다시 쓰기 대상). 근거 카드는 한 장이라도 비면 빈 묶음이다. */
    fun missingFields(): List<String> = listOfNotNull(
        RoastChecks.VERDICT_LINE_FIELD.takeIf { verdictLine == null },
        EvidenceDrafts.FIELD.takeUnless { evidenceFilled },
        ConclusionDraft.FIELD.takeIf { conclusion == null },
    )

    fun isComplete(): Boolean = missingFields().isEmpty()

    /**
     * 빠진 묶음과 직전 응답 [review]의 사유로 다시 쓰기 요청을 만든다. 검증은 통과했지만 받아 둔 문장과 겹쳐 받지 않은 사유([rejections])도 싣고,
     * 받아 둔 판정 한 줄·근거 카드·마지막 한 줄([RoastKept])을 함께 보내 작가가 그것과 겹치지 않게 쓰게 한다.
     */
    fun rewriteFor(review: RoastReview): RoastRewrite {
        val missing = missingFields()
        val reasons = review.checks.reasonsFor(missing + RoastChecks.JSON_FIELD + RoastChecks.DECK_FIELD) + rejections
        return RoastRewrite(missing, reasons, RoastKept(verdictLine, evidence.toKept(), conclusion?.finalLine))
    }

    /**
     * 빈칸을 서버 대체 문장으로 채워 저장할 내용을 만든다. 판정 한 줄은 이 사람에게 정해 준 판정 틀의 대체 문장, 결론 칭호는 [brief] 판정 구간의 고정 문장이다.
     * 근거 카드는 통과한 작가 카드를 쓰고, 빈 슬롯은 칩 후보로 만든 통계 카드(입버릇·튀는 지표 칩)로 채운다.
     * 슬롯이 다 찼어도 근거 카드가 [RoastEvidences.MIN_CARDS]장 아래면 같은 통계 카드로 그 수까지 채운다. 항목 점수 카드는 덱에 실을 때([RoastContent.forDeck])만 쓴다.
     * 작가 문장과 합쳐 덱 전체 유행어가 상한을 넘으면 유행어 없는 고정 문장을 쓴다. 대체 마지막 한 줄은 이 사람의 펀치 틀 후보(시드 순서)에서
     * 판정 한 줄과 펀치가 겹치지 않는 첫 문장이라 사람마다 다르다([RoastBrief.punchFallback]). 후보가 없으면 구간 고정 문장이다.
     */
    fun toContent(brief: RoastBrief, fallback: RoastFallback, rules: RoastRules): RoastContent {
        val grade = brief.verdict.grade()
        val band = fallback.bandOf(grade)
        val kept = evidence.toEvidences(brief)
        val stats = brief.fallbackEvidence(fallback, kept)
        val cards = kept.plus(stats, if (evidenceFilled) RoastEvidences.MIN_CARDS - kept.size() else stats.size())
        val plain = isPlain(brief, band, rules)
        val verdictText = verdictLine ?: fallbackVerdict(brief, band, plain, rules)
        val finalText = conclusion?.finalLine ?: brief.punchFallback(verdictText, plain, rules) ?: band.finalLine(plain)
        return RoastContent(
            brief.verdict.key(),
            RoastText.of(verdictLine, verdictText),
            cards,
            conclusion?.toConclusion(brief) ?: fallback.conclusionOf(grade, cards.hero(), finalText),
        )
    }

    /** 작가 문장과 대체 문장을 합쳐 덱 전체 유행어가 상한을 넘으면 유행어 없는 대체 문장을 쓴다. */
    private fun isPlain(brief: RoastBrief, band: RoastBandCopy, rules: RoastRules): Boolean {
        val verdictText = verdictLine ?: assignedVerdict(brief, band)
        val finalText = conclusion?.finalLine ?: brief.punchFallback(verdictText, false, rules) ?: band.finalLine
        val fallbackSlang = (if (verdictLine == null) rules.slangCountIn(verdictText) else 0) +
            (if (conclusion == null) rules.slangCountIn(finalText) else 0)
        return slangCount(rules) + fallbackSlang > rules.slang.maxPerDeck
    }

    /** 이 사람에게 정해 준 판정 틀의 대체 문장. 정해 준 틀이 없으면 구간 고정 문장이다. 판정 한 줄도 사람마다 다른 유행어 가족을 쓴다. */
    private fun assignedVerdict(brief: RoastBrief, band: RoastBandCopy): String = brief.verdictPunch?.fallback ?: band.verdictLine

    /** 대체 판정 한 줄. [plain]이면 유행어 없는 구간 문장, 아니면 정해 준 판정 틀의 대체 문장이다. 작가 결론 마지막 한 줄과 펀치 표지가 겹치면 유행어 없는 판정 한 줄을 쓴다. */
    private fun fallbackVerdict(brief: RoastBrief, band: RoastBandCopy, plain: Boolean, rules: RoastRules): String {
        val text = if (plain) band.plainVerdictLine else assignedVerdict(brief, band)
        val clashes = conclusion != null && rules.punchClashIn(text, conclusion.finalLine) != null
        return if (clashes) band.plainVerdictLine else text
    }

    /** 응답 근거 카드 [drafts] 가운데 통과한 카드로 빈 슬롯을 채운 시트. 받지 않은 카드의 사유를 남긴다. 이미 다 찼으면 그대로다. */
    private fun withEvidenceFrom(drafts: EvidenceDrafts, checks: RoastChecks, brief: RoastBrief, rules: RoastRules): RoastSheet {
        if (evidenceFilled) return this
        val intake = evidence.absorbed(drafts, checks, brief, rules)
        return copy(evidence = intake.kept, evidenceFilled = intake.kept.fills(brief.slots), rejections = rejections + intake.reasons)
    }

    private fun copy(
        verdictLine: String? = this.verdictLine,
        evidence: EvidenceDrafts = this.evidence,
        evidenceFilled: Boolean = this.evidenceFilled,
        conclusion: ConclusionDraft? = this.conclusion,
        rejections: List<String> = this.rejections,
    ): RoastSheet = RoastSheet(verdictLine, evidence, evidenceFilled, conclusion, rejections)

    private fun slangCount(rules: RoastRules): Int =
        (verdictLine?.let(rules::slangCountIn) ?: 0) + evidence.slangCount(rules) + (conclusion?.slangCount(rules) ?: 0)

    /**
     * 덱 전체 유행어가 상한을 넘거나 판정 한 줄과 결론 마지막 한 줄의 펀치가 겹치면 [previous]에 [field]를 받지 않은 사유를 더해 돌려준다.
     * 더한 것이 없으면(이미 있던 묶음) 그대로다.
     */
    private fun orIfRejected(previous: RoastSheet, rules: RoastRules, field: String): RoastSheet {
        val clash = if (verdictLine != null && conclusion != null) rules.punchClashIn(verdictLine, conclusion.finalLine) else null
        val count = slangCount(rules)
        val reason = when {
            clash != null -> "$field $CLASH_PREFIX($clash)"
            count > rules.slang.maxPerDeck -> "$field 받아 둔 문장과 합쳐 덱 전체 유행어가 ${rules.slang.maxPerDeck}개를 넘어(${count}개) 받지 않았다"
            else -> null
        }
        return if (reason == null) this else previous.copy(rejections = rejections + reason)
    }

    companion object {
        /** 아무것도 통과하지 못한 처음 상태. 작가를 부르지 않으면 이대로 대체 문장만 쓴다. */
        val EMPTY = RoastSheet(null, EvidenceDrafts.EMPTY, false, null)
        private const val CLASH_PREFIX = "판정 한 줄과 결론 마지막 한 줄이 같은 펀치를 써 받지 않았다"
    }
}
