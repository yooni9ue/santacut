package com.santacut.infrastructure.global.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RequiredForeignKeysTest {
    private val sessions = RequiredForeignKey("sessions", "user_id", "participants", "CASCADE")
    private val jobs = RequiredForeignKey("analysis_jobs", "assessment_id", "assessments", "SET NULL")
    private val required = RequiredForeignKeys(listOf(sessions, jobs))

    @Test
    fun `DB에 모두 있으면 빠진 외래 키가 없다`() {
        assertEquals(emptyList(), required.missingFrom(setOf(sessions.describe(), jobs.describe(), "x.y -> z ON DELETE NO ACTION")))
    }

    @Test
    fun `외래 키가 없거나 삭제 규칙이 다르면 빠진 것으로 본다`() {
        val withoutRule = RequiredForeignKey.describe("sessions", "user_id", "participants", "NO ACTION")

        assertEquals(listOf(sessions.describe(), jobs.describe()), required.missingFrom(setOf(withoutRule)))
    }
}
