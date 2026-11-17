package com.santacut.infrastructure.global.config

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import java.io.File
import kotlin.test.assertEquals

/**
 * 빈 DB에서 앱을 띄웠을 때 생기는 제약(외래 키와 삭제 규칙, 인덱스, CHECK)이 기준 스키마와 같은지 확인한다.
 * 컬럼 기본값도 비교한다. 기준은 schema.sql을 지우기 전 그대로 옮긴 `schema-baseline.sql`이다. 운영·dev DB는 이 스크립트로 만들어졌으므로
 * 새로 만든 DB도 같은 제약을 가져야 참가자 삭제와 중복 방지가 두 DB에서 똑같이 동작한다.
 * enum 컬럼에 Hibernate가 상수 목록 CHECK를 붙이면 기준과 달라져 이 테스트가 깨진다([EnumCheckFreeSQLiteDialect]).
 * 빈 DB에서 시작해야 하므로 `build/schema-test.db`를 컨텍스트가 뜨기 전에 지운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./" + SchemaBaselineTest.DB_PATH,
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.scheduling.enabled=false",
    ],
)
class SchemaBaselineTest @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
) {
    @Test
    fun `빈 DB에 만든 테이블의 외래 키, 인덱스, CHECK가 기준 스키마와 같다`() {
        val baseline = shapeOf(baselineDatabase()).mapValues { (_, shape) -> shape.map { BASELINE_DEVIATIONS[it] ?: it } }
        val fresh = shapeOf(jdbcTemplate).mapValues { (table, shape) -> shape - ADDED_AFTER_BASELINE[table].orEmpty() }

        assertEquals(baseline.keys, fresh.keys)
        baseline.keys.forEach { table -> assertEquals(baseline[table], fresh[table], "테이블 $table") }
    }

    private fun baselineDatabase(): JdbcTemplate {
        val dataSource = SingleConnectionDataSource(IN_MEMORY_URL, true)
        val script = ClassPathResource(BASELINE).getContentAsString(Charsets.UTF_8).removePrefix(BOM)
        dataSource.connection.use { ScriptUtils.executeSqlScript(it, ByteArrayResource(script.toByteArray())) }
        return JdbcTemplate(dataSource)
    }

    /** 테이블마다 외래 키, 인덱스, CHECK를 비교하기 쉬운 문장으로 바꿔 정렬한다. */
    private fun shapeOf(database: JdbcTemplate): Map<String, List<String>> =
        database.queryForList(TABLES_SQL, String::class.java).filterNotNull().associateWith { table ->
            (foreignKeys(database, table) + indexes(database, table) + checks(database, table) + defaults(database, table)).sorted()
        }

    private fun foreignKeys(database: JdbcTemplate, table: String): List<String> =
        database.queryForList("SELECT * FROM pragma_foreign_key_list('$table')").map {
            "FK ${it["from"]} -> ${it["table"]}.${it["to"]} ON DELETE ${it["on_delete"]}"
        }

    /** 기본값이 있는 컬럼. 시드 SQL과 손으로 넣는 행은 값을 주지 않은 컬럼을 이 기본값으로 채운다. */
    private fun defaults(database: JdbcTemplate, table: String): List<String> =
        database.queryForList("SELECT name, dflt_value FROM pragma_table_info('$table') WHERE dflt_value IS NOT NULL").map {
            "DEFAULT ${it["name"]} = ${normalize(it["dflt_value"].toString())}"
        }

    /** 이름은 이름 붙인 일반 인덱스만 비교한다. UNIQUE 제약은 기준 스키마에서 자동 인덱스라 이름이 없다. */
    private fun indexes(database: JdbcTemplate, table: String): List<String> =
        database.queryForList("SELECT * FROM pragma_index_list('$table')").map {
            val name = it["name"] as String
            val unique = it["unique"] == 1
            val label = if (unique) "UNIQUE INDEX" else "INDEX $name"
            "$label (${indexColumns(database, name)})${partialCondition(database, name)}"
        }

    private fun indexColumns(database: JdbcTemplate, index: String): String =
        database.queryForList("SELECT name, \"desc\" FROM pragma_index_xinfo('$index') WHERE key = 1 ORDER BY seqno")
            .joinToString(",") { "${it["name"]}${if (it["desc"] == 1) " DESC" else ""}" }

    private fun partialCondition(database: JdbcTemplate, index: String): String {
        val sql = database.queryForList("SELECT sql FROM sqlite_master WHERE name = ?", String::class.java, index)
            .firstOrNull() ?: return ""
        val where = Regex("""(?i)\swhere\s""").find(sql) ?: return ""
        return " WHERE " + normalize(sql.substring(where.range.last + 1))
    }

    private fun checks(database: JdbcTemplate, table: String): List<String> {
        val sql = database.queryForObject("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?", String::class.java, table).orEmpty()
        return CHECK_START.findAll(sql).map { "CHECK " + normalize(balancedBody(sql, it.range.last)) }.toList()
    }

    /** [open] 위치의 여는 괄호와 짝이 맞는 닫는 괄호 사이의 글. */
    private fun balancedBody(sql: String, open: Int): String {
        var depth = 0
        for (index in open until sql.length) {
            if (sql[index] == '(') depth++
            if (sql[index] == ')') depth--
            if (depth == 0) return sql.substring(open + 1, index)
        }
        return sql.substring(open + 1)
    }

    /** 공백과 대소문자, 식 전체를 감싼 괄호 차이는 같은 제약으로 본다. */
    private fun normalize(expression: String): String {
        var result = expression.replace(Regex("""\s+"""), "").lowercase()
        while (result.startsWith("(") && balancedBody(result, 0).length == result.length - 2) {
            result = result.substring(1, result.length - 1)
        }
        return result
    }

    companion object {
        const val DB_PATH = "build/schema-test.db"
        private const val BASELINE = "schema-baseline.sql"
        private const val BOM = "\uFEFF"
        private const val IN_MEMORY_URL = "jdbc:sqlite::memory:"
        private const val TABLES_SQL =
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name"

        /**
         * 기준 스키마와 일부러 다르게 만드는 제약. 공유 취소는 행을 지우므로 shares.revoked_at은 늘 NULL이고 엔티티에 없다.
         * 그래서 새 DB의 유일 인덱스는 WHERE 없이 만든다. 예전 DB의 부분 인덱스와 막는 중복은 같다.
         */
        private val BASELINE_DEVIATIONS = mapOf(
            "UNIQUE INDEX (user_id,snapshot_id) WHERE revoked_atisnull" to "UNIQUE INDEX (user_id,snapshot_id)",
        )

        /** schema.sql 밖에서 Hibernate가 나중에 더한 컬럼의 제약. 기준 스키마에는 그 컬럼이 없다. */
        private val ADDED_AFTER_BASELINE = mapOf("ai_usage_days" to setOf("DEFAULT cached_input_tokens = 0"))
        private val CHECK_START = Regex("""(?i)\bcheck\s*\(""")

        @JvmStatic
        @BeforeAll
        fun startFromEmptyDatabase() {
            File(DB_PATH).delete()
        }
    }
}
