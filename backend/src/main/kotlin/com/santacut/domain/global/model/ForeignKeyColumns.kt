package com.santacut.domain.global.model

/**
 * 다른 테이블을 id로 가리키는 컬럼의 정의(`@Column(columnDefinition = ...)`).
 * Hibernate의 SQLite 방언은 외래 키를 만들지 않으므로 REFERENCES와 삭제 규칙을 컬럼 정의에 직접 적는다.
 * 참가자 행 하나를 지우면 DB가 삭제 규칙을 따라 그 참가자의 행을 모두 지운다. NOT NULL은 `nullable = false`가 붙인다.
 */
object ForeignKeyColumns {
    const val PARTICIPANT = "TEXT REFERENCES participants(id) ON DELETE CASCADE"
    const val ASSESSMENT = "TEXT REFERENCES assessments(id) ON DELETE CASCADE"

    /** 평가가 지워져도 작업 기록은 남기고 평가 id만 비운다. */
    const val ASSESSMENT_OR_NULL = "TEXT REFERENCES assessments(id) ON DELETE SET NULL"
    const val ANALYSIS_JOB = "TEXT REFERENCES analysis_jobs(id) ON DELETE CASCADE"
    const val RESULT_SNAPSHOT = "TEXT REFERENCES result_snapshots(id) ON DELETE CASCADE"

    /** 지역은 참가자가 가리키는 동안 지우지 않는다(삭제 규칙 없음). */
    const val REGION = "TEXT REFERENCES regions(code)"
}
