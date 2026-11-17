package com.santacut.infrastructure.global.config

import org.hibernate.community.dialect.SQLiteDialect
import org.hibernate.type.descriptor.jdbc.JdbcType

/**
 * enum 컬럼에 상수 목록 CHECK를 붙이지 않는 SQLite 방언. `spring.jpa.database-platform`이 이 클래스를 가리킨다.
 *
 * 기본 방언은 새로 만드는 enum 컬럼에 `CHECK (col IN (지금 상수들))`을 붙인다. `ddl-auto: update`는 이 CHECK를 고치지 않고
 * SQLite는 테이블을 다시 만들지 않고는 제약을 지울 수 없다. 그래서 상수를 하나 더하면 그 뒤 새로 만든 DB에서만 INSERT가 깨진다.
 * 운영·dev DB는 schema.sql로 만들어 enum CHECK가 없으므로 새 DB도 같게 맞춘다. AttributeConverter를 써도 CHECK는 생긴다.
 * 엔티티에 직접 적은 `@Check`는 그대로 만든다.
 */
class EnumCheckFreeSQLiteDialect : SQLiteDialect() {
    override fun getCheckCondition(columnName: String, enumClass: Class<out Enum<*>>): String? = null

    override fun getCheckCondition(columnName: String, values: Array<String>): String? = null

    override fun getCheckCondition(columnName: String, values: Collection<*>, jdbcType: JdbcType): String? = null
}
