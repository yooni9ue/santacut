package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class AnalysisJobsTest {
    private val participant = UUID.randomUUID()
    private val first = job(participant)
    private val second = job(participant)

    @Test
    fun `작업을 낸 참가자 id는 겹치지 않는다`() {
        assertEquals(setOf(participant.toString()), AnalysisJobs(listOf(first, second)).userIds())
    }

    @Test
    fun `읽은 순서대로 작업을 바꾼다`() {
        assertEquals(listOf(first.id, second.id), AnalysisJobs(listOf(first, second)).map(AnalysisJob::id))
    }

    private fun job(participantId: UUID): AnalysisJob = AnalysisJob.enqueue(
        participantId,
        IdempotencyKey.from("key-" + UUID.randomUUID()),
        AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
        AnalysisPayload("{}"),
        Instant.EPOCH,
    )
}
