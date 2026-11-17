package com.santacut.infrastructure.analysis.repository

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisJobChange
import com.santacut.domain.analysis.model.AnalysisJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AnalysisJobRepository : JpaRepository<AnalysisJob, String> {
    fun findByUserIdAndIdempotencyKey(userId: String, idempotencyKey: String): AnalysisJob?

    fun findByIdAndUserId(id: String, userId: String): AnalysisJob?

    fun findFirstByUserIdOrderByCreatedAtDesc(userId: String): AnalysisJob?

    fun countByUserIdAndCreatedAtAfter(userId: String, createdAt: Instant): Long

    fun findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId: String, createdAt: Instant): AnalysisJob?

    fun findFirstByStatusOrderByCreatedAtAsc(status: AnalysisJobStatus): AnalysisJob?

    fun findFirstByUserIdAndDigestHashAndFinishedAtIsNotNullOrderByFinishedAtDesc(
        userId: String,
        digestHash: String,
    ): AnalysisJob?

    fun findFirstByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
        statuses: Collection<AnalysisJobStatus>,
        createdAt: Instant,
    ): AnalysisJob?

    fun findFirstByStatusAndLeaseUntilBeforeOrderByCreatedAtAsc(status: AnalysisJobStatus, leaseUntil: Instant): AnalysisJob?

    /** [ids] 작업을 끝난 순서대로 한 번에 읽는다. */
    fun findByIdInOrderByFinishedAtAsc(ids: Collection<String>): List<AnalysisJob>

    /** [since]와 같거나 그 뒤에 [status]로 끝난 작업마다 저장 알림 하나씩. */
    @Query(
        "select new com.santacut.domain.analysis.model.AnalysisJobChange(j.id) from AnalysisJob j " +
            "where j.status = :status and j.finishedAt >= :since",
    )
    fun findChangesFinishedSince(
        @Param("status") status: AnalysisJobStatus,
        @Param("since") since: Instant,
    ): List<AnalysisJobChange>
}
