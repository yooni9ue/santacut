package com.santacut.infrastructure.global.config

/**
 * 앱이 뜰 때 확인하는 외래 키 목록.
 */
class RequiredForeignKeys(private val values: List<RequiredForeignKey>) {
    /** [present](DB에 있는 외래 키 설명) 안에 없는 외래 키의 설명. */
    fun missingFrom(present: Set<String>): List<String> = values.map(RequiredForeignKey::describe).filterNot(present::contains)
}
