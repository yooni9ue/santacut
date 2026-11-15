package com.santacut.domain.model

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

    companion object {
        fun record(job: AnalysisJob, score: Score, componentsJson: String, recordedAt: Instant): Assessment {
            val verdict = score.verdict()
            return Assessment(
                UUID.randomUUID().toString(), job.userId, job.source, score.hundredths,
                verdict.title, verdict.comment, componentsJson, recordedAt,
            )
        }
    }
}
