package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.global.model.ForeignKeyColumns
import com.santacut.domain.global.model.UtcDatabaseTime
import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 평가 하나의 조서 집계(「2026 산타 조서」). 채점을 끝낸 트랜잭션에서 함께 저장하고, 장면은 조회할 때 리소스 템플릿으로 조립한다.
 * 평가나 참가자를 지우면 외래 키 삭제 규칙으로 함께 지워진다. 평가 하나에 조서 하나다(seed.sql의 유일 인덱스).
 *
 * @property statsJson 집계값([DossierStats]) JSON. 원문, 이름, 링크, 전화번호는 담지 않는다.
 * @property tone 위기 표현 기준으로 정한 톤. 위기 표현 횟수는 저장하지 않는다.
 * @property statsVersion 집계 규칙 판([DossierStats.VERSION])
 */
@Entity
@Table(name = "dossiers", indexes = [Index(name = "dossiers_user", columnList = "user_id")])
class Dossier(
    id: String,
    assessmentId: String,
    userId: String,
    statsJson: String,
    tone: DossierTone,
    statsVersion: Int,
    createdAt: Instant,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "assessment_id", nullable = false, columnDefinition = ForeignKeyColumns.ASSESSMENT)
    var assessmentId: String = assessmentId
        protected set

    @Column(name = "user_id", nullable = false, columnDefinition = ForeignKeyColumns.PARTICIPANT)
    var userId: String = userId
        protected set

    @Column(name = "stats_json", nullable = false)
    var statsJson: String = statsJson
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false)
    var tone: DossierTone = tone
        protected set

    @Column(name = "stats_version", nullable = false)
    var statsVersion: Int = statsVersion
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false, columnDefinition = UtcDatabaseTime.CREATED_AT_COLUMN_DEFINITION)
    var createdAt: Instant = createdAt
        protected set

    /** 지금 집계 규칙 판으로 센 조서인지. 같은 입력 재사용 때 판이 다르면 복사하지 않고 다시 센다. */
    fun isCurrentVersion(): Boolean = statsVersion == DossierStats.VERSION

    /** 같은 채점 입력으로 다시 낸 새 평가 [assessment]에 이 집계를 옮겨 적는다. */
    fun copyFor(assessment: Assessment, copiedAt: Instant): Dossier =
        Dossier(UUID.randomUUID().toString(), assessment.id, assessment.userId, statsJson, tone, statsVersion, copiedAt)

    companion object {
        /** [assessment]의 조서. [statsJson]은 [tally]의 집계값을 JSON으로 쓴 것이다. */
        fun record(assessment: Assessment, tally: DossierTally, statsJson: String, recordedAt: Instant): Dossier = Dossier(
            UUID.randomUUID().toString(), assessment.id, assessment.userId, statsJson, tally.tone, tally.stats.version, recordedAt,
        )
    }
}
