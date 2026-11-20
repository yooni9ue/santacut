package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.global.model.ForeignKeyColumns
import com.santacut.domain.global.model.UtcDatabaseTime
import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 평가 하나의 팩폭 결과(「팩폭 산타」 작가 문장). 채점을 끝낸 트랜잭션에서 함께 저장한다. 평가 하나에 하나다(seed.sql의 유일 인덱스).
 * 평가나 참가자를 지우면 외래 키 삭제 규칙으로 함께 지워진다.
 *
 * @property contentJson 검증을 통과한 문장과 대체 문장, 필드별 출처([RoastContent]) JSON. 말풍선 원문은 담지 않는다.
 * @property promptVersion 작가 프롬프트 판. 작가를 부르지 않았으면(꺼짐, 프롬프트 없음, 예산 막힘, 기능 전 평가 재사용) null이다.
 * @property model 작가 모델 ID. 작가를 부르지 않았으면 빈 문자열이다.
 */
@Entity
@Table(name = "roasts", indexes = [Index(name = "roasts_user", columnList = "user_id")])
class Roast(
    id: String,
    assessmentId: String,
    userId: String,
    contentJson: String,
    promptVersion: Int?,
    model: String,
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

    @Column(name = "content_json", nullable = false)
    var contentJson: String = contentJson
        protected set

    @Column(name = "prompt_version")
    var promptVersion: Int? = promptVersion
        protected set

    @Column(name = "model", nullable = false)
    var model: String = model
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false, columnDefinition = UtcDatabaseTime.CREATED_AT_COLUMN_DEFINITION)
    var createdAt: Instant = createdAt
        protected set

    /** 같은 채점 입력으로 다시 낸 새 평가 [assessment]에 이 결과를 옮겨 적는다. */
    fun copyFor(assessment: Assessment, copiedAt: Instant): Roast =
        Roast(UUID.randomUUID().toString(), assessment.id, assessment.userId, contentJson, promptVersion, model, copiedAt)

    companion object {
        /** [assessment]의 팩폭 결과. 문장과 JSON은 [draft]에 미리 만들어 둔 것이다. */
        fun record(assessment: Assessment, draft: RoastDraft, recordedAt: Instant): Roast = Roast(
            UUID.randomUUID().toString(), assessment.id, assessment.userId, draft.contentJson, draft.promptVersion, draft.model, recordedAt,
        )
    }
}
