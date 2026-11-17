package com.santacut.infrastructure.global.config

/**
 * DB에 꼭 있어야 하는 외래 키 하나. `PRAGMA foreign_key_list`의 한 줄과 같은 글로 바꿔 비교한다.
 *
 * @property onDelete 참조 행이 지워질 때 규칙(`CASCADE`, `SET NULL`)
 */
data class RequiredForeignKey(
    val table: String,
    val column: String,
    val referencedTable: String,
    val onDelete: String,
) {
    fun describe(): String = describe(table, column, referencedTable, onDelete)

    companion object {
        fun describe(table: String, column: String, referencedTable: String, onDelete: String): String =
            "$table.$column -> $referencedTable ON DELETE $onDelete"
    }
}
