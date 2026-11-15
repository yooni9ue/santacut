package com.santacut.infrastructure.analysis.repository

import com.santacut.domain.analysis.model.AnalysisUpload
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface AnalysisUploadRepository : JpaRepository<AnalysisUpload, String> {
    fun findFirstByOrderByCreatedAtAsc(): AnalysisUpload?

    /** 원본을 읽어 오지 않고 바로 지운다. */
    @Modifying
    @Query("delete from AnalysisUpload upload where upload.jobId = :jobId")
    fun deleteByJobId(jobId: String): Int

    /** 이 시각보다 먼저 받은 원본을 읽어 오지 않고 한 번에 지운다. */
    @Modifying
    @Query("delete from AnalysisUpload upload where upload.createdAt < :cutoff")
    fun deleteByCreatedAtBefore(cutoff: Instant): Int
}
