package com.santacut.infrastructure.analysis.repository

import com.santacut.domain.analysis.model.Assessment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AssessmentRepository : JpaRepository<Assessment, String> {
    /** 참가자마다 가장 최근 평가 하나씩. 같은 시각이면 ID가 큰 쪽을 최신으로 본다. */
    @Query(
        """
        select a from Assessment a
        where not exists (
            select newer.id from Assessment newer
            where newer.userId = a.userId
              and (newer.createdAt > a.createdAt or (newer.createdAt = a.createdAt and newer.id > a.id))
        )
        """,
    )
    fun findLatestForEachUser(): List<Assessment>
}
