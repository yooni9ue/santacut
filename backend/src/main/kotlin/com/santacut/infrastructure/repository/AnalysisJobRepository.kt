package com.santacut.infrastructure.repository

import com.santacut.domain.model.AnalysisJob
import com.santacut.domain.model.AnalysisJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface AnalysisJobRepository : JpaRepository<AnalysisJob, String> {
    fun findByUserIdAndIdempotencyKey(userId: String, idempotencyKey: String): AnalysisJob?

    fun findByIdAndUserId(id: String, userId: String): AnalysisJob?

    fun countByUserIdAndCreatedAtAfter(userId: String, createdAt: Instant): Long

    fun findFirstByStatusOrderByCreatedAtAsc(status: AnalysisJobStatus): AnalysisJob?

    fun findFirstByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
        status: AnalysisJobStatus,
        createdAt: Instant,
    ): AnalysisJob?

    fun findFirstByStatusAndLeaseUntilBeforeAndAttemptsGreaterThanEqualOrderByCreatedAtAsc(
        status: AnalysisJobStatus,
        leaseUntil: Instant,
        attempts: Int,
    ): AnalysisJob?

    fun findFirstByStatusAndLeaseUntilBeforeAndAttemptsLessThanOrderByCreatedAtAsc(
        status: AnalysisJobStatus,
        leaseUntil: Instant,
        attempts: Int,
    ): AnalysisJob?
}
