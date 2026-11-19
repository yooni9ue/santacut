package com.santacut.infrastructure.global.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.jdbc.core.JdbcTemplate

/**
 * 앱이 뜰 때 참가자 삭제가 기대는 외래 키 삭제 규칙이 DB에 모두 있는지 확인하고, 하나라도 없으면 앱을 띄우지 않는다.
 *
 * `ddl-auto: update`는 이미 있는 테이블에 외래 키를 더하지 않는다. 엔티티의 컬럼 정의(`ForeignKeyColumns`)를 잘못 고치거나
 * 규칙 없이 만든 DB로 뜨면 참가자를 지워도 행이 남는다. 그런 DB는 조용히 돌지 않고 여기서 멈춘다.
 * Hibernate가 테이블을 만든 뒤에 확인하도록 JPA(`entityManagerFactory`) 빈 다음에 만든다.
 * 삭제 규칙이 없는 외래 키(participants.region_code, regions.parent_code)는 예전 DB에 없을 수 있어 확인하지 않는다.
 */
@Configuration
@DependsOn(ForeignKeyCheckConfiguration.ENTITY_MANAGER_FACTORY)
class ForeignKeyCheckConfiguration(private val jdbcTemplate: JdbcTemplate) {
    @PostConstruct
    fun verifyDeleteRules() {
        val missing = REQUIRED.missingFrom(presentForeignKeys())
        if (missing.isNotEmpty()) throw IllegalStateException(MISSING_MESSAGE + missing.joinToString(", "))
    }

    private fun presentForeignKeys(): Set<String> =
        jdbcTemplate.query(FOREIGN_KEYS_SQL) { row, _ ->
            RequiredForeignKey.describe(row.getString("tbl"), row.getString("col"), row.getString("ref"), row.getString("on_delete"))
        }.toSet()

    companion object {
        const val ENTITY_MANAGER_FACTORY = "entityManagerFactory"
        private const val CASCADE = "CASCADE"
        private const val SET_NULL = "SET NULL"
        private const val PARTICIPANTS = "participants"
        private const val FOREIGN_KEYS_SQL =
            "SELECT m.name AS tbl, f.\"from\" AS col, f.\"table\" AS ref, f.on_delete " +
                "FROM sqlite_master m JOIN pragma_foreign_key_list(m.name) f WHERE m.type = 'table'"
        private const val MISSING_MESSAGE =
            "DB에 외래 키 삭제 규칙이 없어 앱을 띄우지 않는다. 참가자를 지워도 행이 남는다. " +
                "deployment.md의 DB 확인 절차를 따른다. 없는 규칙: "

        private val REQUIRED = RequiredForeignKeys(
            listOf(
                RequiredForeignKey("sessions", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("region_changes", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("assessments", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("analysis_jobs", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("analysis_jobs", "assessment_id", "assessments", SET_NULL),
                RequiredForeignKey("analysis_uploads", "job_id", "analysis_jobs", CASCADE),
                RequiredForeignKey("result_snapshots", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("result_snapshots", "assessment_id", "assessments", CASCADE),
                RequiredForeignKey("shares", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("shares", "snapshot_id", "result_snapshots", CASCADE),
                RequiredForeignKey("dossiers", "user_id", PARTICIPANTS, CASCADE),
                RequiredForeignKey("dossiers", "assessment_id", "assessments", CASCADE),
            ),
        )
    }
}
