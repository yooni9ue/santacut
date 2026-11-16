package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 분석 작업이 성공해 확정된 점수 평가.
 *
 * @property components 항목별 점수를 담은 JSON 배열
 * @property promptVersion 점수를 매길 때 쓴 채점 프롬프트(`score`) 판 번호. 프롬프트를 DB로 옮기기 전 평가는 null이다.
 * @property lowConfidence 근거가 부족해 중립 점수를 준 항목 키를 쉼표로 이은 값([LowConfidenceDimensions]). 이 값이 생기기 전 평가는 null이다.
 * @property playfulness 순위에 쓰지 않는 장난기 성향(0~100). 유형을 고를 때 쓴다. 이 값이 생기기 전 평가는 null이다.
 * @property scoreRuleVersion 점수 규칙 판([Score.RULE_VERSION]). 1판(구간 합산·수축 전) 평가는 null이다.
 * @property quote 본인 근거 줄에서 고른 20자 이하 한마디. 본인 결과에만 보이고 공유 카드에는 본인이 고를 때만 싣는다.
 * @property badges 본인 말에서 받은 배지([EarnedBadges]). 이 값이 생기기 전 평가는 null이다.
 * @property rawComponents 수축 전 항목별 평균을 담은 JSON 배열. 유형을 고를 때만 쓴다. 이 값이 생기기 전 평가는 null이다.
 */
@Entity
@Table(name = "assessments")
class Assessment(
    id: String,
    userId: String,
    source: DataSource,
    scoreHundredths: Int,
    verdict: String,
    comment: String,
    components: String,
    createdAt: Instant,
    promptVersion: Int? = null,
    lowConfidence: String? = null,
    playfulness: Int? = null,
    scoreRuleVersion: Int? = null,
    quote: String? = null,
    badges: String? = null,
    rawComponents: String? = null,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    var source: DataSource = source
        protected set

    @Column(name = "score_hundredths", nullable = false)
    var scoreHundredths: Int = scoreHundredths
        protected set

    @Column(name = "verdict", nullable = false)
    var verdict: String = verdict
        protected set

    @Column(name = "comment", nullable = false)
    var comment: String = comment
        protected set

    @Column(name = "components", nullable = false)
    var components: String = components
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = createdAt
        protected set

    @Column(name = "prompt_version")
    var promptVersion: Int? = promptVersion
        protected set

    @Column(name = "low_confidence")
    var lowConfidence: String? = lowConfidence
        protected set

    @Column(name = "playfulness")
    var playfulness: Int? = playfulness
        protected set

    @Column(name = "score_rule_version")
    var scoreRuleVersion: Int? = scoreRuleVersion
        protected set

    @Column(name = "quote")
    var quote: String? = quote
        protected set

    @Column(name = "badges")
    var badges: String? = badges
        protected set

    @Column(name = "raw_components")
    var rawComponents: String? = rawComponents
        protected set

    /** 저장한 항목 점수. [ScoreDimension] 순서다. */
    fun dimensionScores(): ScoreComponents = ScoreComponents.fromColumn(components)

    /** 장난기 기준값 [playfulnessSplit](참가자 중앙값)으로 고른 유형. */
    fun archetype(playfulnessSplit: Int): Archetype =
        Archetype.of(dimensionScores(), playfulness, playfulnessSplit, rawComponents?.let(ScoreComponents::fromColumn))

    /** 받은 배지 라벨 목록. */
    fun badgeLabels(): List<String> = EarnedBadges.fromColumn(badges).labels()

    /** 근거가 부족해 중립 점수를 준 항목 키 목록. */
    fun lowConfidenceKeys(): List<String> = LowConfidenceDimensions.fromColumn(lowConfidence).toKeys()

    companion object {
        /** [componentsJson]은 수축한 항목 점수, [rawComponentsJson]은 수축 전 평균의 JSON 배열이다. */
        fun record(job: AnalysisJob, scored: ScoredAssessment, componentsJson: String, rawComponentsJson: String, recordedAt: Instant): Assessment {
            val score = scored.score
            val verdict = score.verdict()
            return Assessment(
                UUID.randomUUID().toString(), job.userId, job.source, score.hundredths,
                verdict.title, verdict.comment, componentsJson, recordedAt, scored.promptVersion, score.lowConfidence.toColumn(),
                score.playfulness, Score.RULE_VERSION, scored.quote, scored.badges.toColumn(), rawComponentsJson,
            )
        }

        /** 같은 채점 입력으로 이미 받은 평가([previous])를 AI 없이 이 작업의 새 평가로 옮겨 적는다. */
        fun reuse(job: AnalysisJob, previous: Assessment, recordedAt: Instant): Assessment = Assessment(
            UUID.randomUUID().toString(), job.userId, job.source, previous.scoreHundredths,
            previous.verdict, previous.comment, previous.components, recordedAt, previous.promptVersion, previous.lowConfidence,
            previous.playfulness, previous.scoreRuleVersion, previous.quote, previous.badges, previous.rawComponents,
        )
    }
}
