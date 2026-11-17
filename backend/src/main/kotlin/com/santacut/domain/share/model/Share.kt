package com.santacut.domain.share.model

import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.ArchetypeConverter
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.analysis.model.ScoreDimensionKeyConverter
import com.santacut.domain.global.model.RandomToken
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.global.model.UtcInstantConverter
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.ranking.model.RankingStage
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 공개 공유 링크. 만든 순간의 결과를 그대로 복사해 두어 이후 순위가 바뀌어도 내용이 변하지 않는다.
 * 취소하면 행을 지운다(소프트 삭제를 쓰지 않는다).
 * 카드에는 상대 말을 싣지 않는다. 본인 한마디([quote])도 본인이 고를 때만 싣는다.
 *
 * @property archetype 공유 당시 산타 유형. 이 컬럼이 생기기 전 링크와 지금은 없는 유형 코드는 null이다.
 * @property topPercent 공유 당시 상위 몇 %. 참가자 100명 미만이었으면 null이다.
 * @property badge 공유 당시 첫 배지 라벨
 * @property weakestDimension 공유 당시 가장 약한 항목. 컬럼에는 항목 키로 저장한다.
 * @property dimensionScores 공유 당시 항목 점수 JSON 배열
 * @property quote 본인이 싣기로 고른 한마디. 고르지 않았으면 null이다.
 */
@Entity
@Table(name = "shares")
class Share(
    token: String,
    userId: String,
    snapshotId: String,
    nickname: String,
    scoreHundredths: Int,
    rank: Int,
    eta: Instant,
    verdict: String,
    comment: String,
    computedAt: Instant,
    tied: Boolean = false,
    stage: RankingStage = RankingStage.PREVIEW,
    createdAt: Instant? = null,
    archetype: Archetype? = null,
    topPercent: Int? = null,
    badge: String? = null,
    weakestDimension: ScoreDimension? = null,
    dimensionScores: String? = null,
    quote: String? = null,
) {
    @Id
    @Column(name = "token")
    var token: String = token
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String = userId
        protected set

    @Column(name = "snapshot_id", nullable = false)
    var snapshotId: String = snapshotId
        protected set

    @Column(name = "nickname", nullable = false)
    var nickname: String = nickname
        protected set

    @Column(name = "score_hundredths", nullable = false)
    var scoreHundredths: Int = scoreHundredths
        protected set

    @Column(name = "rank", nullable = false)
    var rank: Int = rank
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "eta", nullable = false)
    var eta: Instant = eta
        protected set

    @Column(name = "verdict", nullable = false)
    var verdict: String = verdict
        protected set

    @Column(name = "comment", nullable = false)
    var comment: String = comment
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "computed_at", nullable = false)
    var computedAt: Instant = computedAt
        protected set

    /** 공유 당시 공동 순위였는지. 이 컬럼이 없던 기존 DB에도 붙도록 기본값을 둔다. */
    @Column(name = "tied", nullable = false, columnDefinition = TIED_COLUMN_DEFINITION)
    var tied: Boolean = tied
        protected set

    /** 공유 당시 순위 상태. 이 컬럼이 없던 기존 DB에도 붙도록 기본값을 둔다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, columnDefinition = STAGE_COLUMN_DEFINITION)
    var stage: RankingStage = stage
        protected set

    /**
     * 공유 링크를 만든 시각. SQLite는 시각 함수 기본값으로 컬럼을 더할 수 없어 기존 DB의 행은 비어 있다.
     * 비어 있으면 [sharedAt]이 계산 시각으로 대신한다.
     */
    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", columnDefinition = CREATED_AT_COLUMN_DEFINITION)
    var createdAt: Instant? = createdAt
        protected set

    @Convert(converter = ArchetypeConverter::class)
    @Column(name = "archetype")
    var archetype: Archetype? = archetype
        protected set

    @Column(name = "top_percent")
    var topPercent: Int? = topPercent
        protected set

    @Column(name = "badge")
    var badge: String? = badge
        protected set

    @Convert(converter = ScoreDimensionKeyConverter::class)
    @Column(name = "weakest_dimension")
    var weakestDimension: ScoreDimension? = weakestDimension
        protected set

    @Column(name = "dimension_scores")
    var dimensionScores: String? = dimensionScores
        protected set

    @Column(name = "quote")
    var quote: String? = quote
        protected set

    /** 본인 한마디를 싣고 있는지. */
    fun carriesQuote(): Boolean = quote != null

    /** 본인 한마디([assessmentQuote])를 [includeQuote]일 때만 싣는다. 다른 값은 공유한 순간 값 그대로 둔다. */
    fun chooseQuote(assessmentQuote: String?, includeQuote: Boolean) {
        quote = assessmentQuote.takeIf { includeQuote }
    }

    /** 산타 유형 이름. 예전 링크면 빈 문자열이다. */
    fun displayArchetype(): String = archetype?.koreanName.orEmpty()

    /** `공감 72 · 응원 65 · 존중 70 · 협력 58` 같은 항목 막대 요약. 예전 링크면 빈 문자열이다. */
    fun displayBars(): String = dimensionScores?.let { ScoreComponents.fromColumn(it).toBarText() }.orEmpty()

    /** 배지, 상위 %, 가장 약한 항목을 ` · `로 이은 줄. 값이 없는 것은 뺀다. */
    fun displayHighlights(): String = listOfNotNull(
        badge,
        topPercent?.let { "$TOP_PREFIX$it$PERCENT_SUFFIX" },
        weakestDimension?.let { "$WEAKEST_PREFIX${it.label}" },
    ).joinToString(HIGHLIGHT_SEPARATOR)

    /** 소수점 둘째 자리까지 표시한 점수(예: `82.45`). */
    fun displayScore(): String =
        BigDecimal(scoreHundredths).movePointLeft(SCORE_DECIMAL_PLACES).setScale(SCORE_DECIMAL_PLACES).toPlainString()

    /** 서울 날짜로 표시한 도착 예정일. 공유 화면은 누구나 보므로 시각은 빼서 지역 사이 거리를 거꾸로 셈하지 못하게 한다. */
    fun displayEta(): String = eta.atZone(SeoulTime.ZONE).format(SEOUL_DATE_FORMAT)

    /** 공동 순위면 `공동 3위`, 아니면 `3위`. */
    fun displayRank(): String = if (tied) "$TIED_PREFIX$rank$RANK_SUFFIX" else "$rank$RANK_SUFFIX"

    /** 서울 시각으로 표시한 순위 계산 시각. */
    fun displayComputedAt(): String = formatSeoul(computedAt)

    /** 공유 당시 순위 상태 이름. */
    fun displayStage(): String = stage.label

    /** 서울 시각으로 표시한 공유 시각. */
    fun displaySharedAt(): String = formatSeoul(sharedAt())

    /** 공유 링크를 만든 시각. 기존 DB의 행처럼 비어 있으면 계산 시각을 쓴다. */
    fun sharedAt(): Instant = createdAt ?: computedAt

    private fun formatSeoul(instant: Instant): String = instant.atZone(SeoulTime.ZONE).format(SEOUL_FORMAT)

    companion object {
        private const val SCORE_DECIMAL_PLACES = 2
        private const val TIED_PREFIX = "공동 "
        private const val RANK_SUFFIX = "위"
        private const val TIED_COLUMN_DEFINITION = "INTEGER NOT NULL DEFAULT 0"
        private const val STAGE_COLUMN_DEFINITION = "TEXT NOT NULL DEFAULT 'PREVIEW'"
        private const val CREATED_AT_COLUMN_DEFINITION = "TEXT"
        private const val TOP_PREFIX = "상위 "
        private const val PERCENT_SUFFIX = "%"
        private const val WEAKEST_PREFIX = "아쉬운 항목 "
        private const val HIGHLIGHT_SEPARATOR = " · "
        private val SEOUL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")
        private val SEOUL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

        /**
         * 공유 링크를 만든다. 순위, 공동 순위 여부, 상태, 시각, 유형과 꾸밈 값은 만든 순간 값으로 고정한다.
         * 본인 한마디는 [includeQuote]가 참일 때만 싣는다.
         */
        fun issue(
            participant: Participant,
            snapshot: ResultSnapshot,
            assessment: Assessment,
            stage: RankingStage,
            issuedAt: Instant,
            profile: ResultProfile,
            includeQuote: Boolean,
        ): Share = Share(
            RandomToken.generate(), participant.id, snapshot.id, participant.nickname, assessment.scoreHundredths,
            snapshot.rank, snapshot.eta, assessment.verdict, assessment.comment, snapshot.computedAt,
            snapshot.tied, stage, issuedAt, profile.archetype, profile.topPercent, profile.firstBadge(),
            profile.weakestDimension(), assessment.components, profile.quote.takeIf { includeQuote },
        )
    }
}
