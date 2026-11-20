package com.santacut.domain.roast.model

/**
 * 작가 호출을 거듭하며 모은, 검증을 통과한 문장. 처음 통과한 문장을 지키고 빠진 필드만 다음 응답에서 채운다.
 * 짝은 사실 key가 겹치지 않게 [MAX_PAIRS]개까지 모으고 [MIN_PAIRS]개 이상이면 다시 쓰지 않는다. 줄거리는 막마다 한 줄이다.
 */
class RoastSheet(
    private val title: String?,
    private val verdict: String?,
    private val verdictMild: String?,
    private val pairs: RoastPairs,
    private val letter: String?,
    private val story: RoastStoryLines,
) {
    /** 응답 [review]에서 통과한 필드로 빈칸을 채운다. 읽을 수 없는 응답이면 그대로다. */
    fun absorb(review: RoastReview): RoastSheet {
        val output = review.output ?: return this
        val checks = review.checks
        return RoastSheet(
            title ?: output.title.takeIf { checks.passed(TITLE) },
            verdict ?: output.verdict.takeIf { checks.passed(VERDICT) },
            verdictMild ?: output.verdictMild.takeIf { checks.passed(VERDICT_MILD) },
            pairs.plus(output.pairs.passing(checks)),
            letter ?: output.letter.takeIf { checks.passed(LETTER) },
            story.plus(output.story.passing(checks)),
        )
    }

    /** 아직 채우지 못한 필드 이름(다시 쓰기 대상). */
    fun missingFields(): List<String> = listOfNotNull(
        TITLE.takeIf { title == null },
        VERDICT.takeIf { verdict == null },
        VERDICT_MILD.takeIf { verdictMild == null },
        RoastPairs.FIELD.takeIf { pairs.size() < MIN_PAIRS },
        LETTER.takeIf { letter == null },
        RoastStoryLines.FIELD.takeIf { story.size() < ACTS },
    )

    fun isComplete(): Boolean = missingFields().isEmpty()

    /** 빠진 필드와 직전 응답 [review]의 사유, 이미 쓴 사실 key로 다시 쓰기 요청을 만든다. */
    fun rewriteFor(review: RoastReview): RoastRewrite {
        val missing = missingFields()
        return RoastRewrite(missing, review.checks.reasonsFor(missing + RoastChecks.JSON_FIELD), pairs.factKeys())
    }

    /** 빈칸을 대체 문장([fallback], [seed]로 고름)으로 채워 저장할 내용을 만든다. 짝에는 [brief] 사실의 서버 숫자 칩을 붙인다. */
    fun toContent(brief: RoastBrief, fallback: RoastFallback, seed: Int): RoastContent {
        val passed = brief.verdict.passed
        return RoastContent(
            brief.verdict.key(),
            RoastText.of(title, fallback.titles.pick(passed, seed)),
            RoastText.of(verdict, fallback.verdicts.pick(passed, seed)),
            RoastText.of(verdictMild, fallback.verdictsMild.pick(passed, seed)),
            pairs.completedWith(brief.facts, fallback),
            RoastText.of(letter, fallback.letters.pick(passed, seed)),
            story.completedWith(fallback, seed),
        )
    }

    companion object {
        const val TITLE = "title"
        const val VERDICT = "verdict"
        const val VERDICT_MILD = "verdict_mild"
        const val LETTER = "letter"

        /** 화면에 쓰는 짝 수. 이만큼 모이면 다시 쓰지 않고, 모자라면 대체 짝으로 채운다. */
        const val MIN_PAIRS = 3

        /** 작가에게 받는 짝 수(나머지는 예비). */
        const val MAX_PAIRS = 5
        const val ACTS = 3

        /** 아무것도 통과하지 못한 처음 상태. 작가를 부르지 않으면 이대로 대체 문장만 쓴다. */
        val EMPTY = RoastSheet(null, null, null, RoastPairs.EMPTY, null, RoastStoryLines.EMPTY)
    }
}
