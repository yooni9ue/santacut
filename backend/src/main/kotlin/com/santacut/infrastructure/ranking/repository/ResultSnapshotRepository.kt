package com.santacut.infrastructure.ranking.repository

import com.santacut.domain.ranking.model.RankingRow
import com.santacut.domain.ranking.model.ResultSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ResultSnapshotRepository : JpaRepository<ResultSnapshot, String> {
    fun findFirstByUserIdOrderByComputedAtDesc(userId: String): ResultSnapshot?

    fun findFirstByOrderByComputedAtDesc(): ResultSnapshot?

    fun countByComputedAt(computedAt: Instant): Long

    /** 한 번의 순위 계산 결과를 배송 순서가 afterSequence보다 뒤인 줄부터 limit줄 읽는다. */
    @Query(
        """
        select new com.santacut.domain.ranking.model.RankingRow(
            p.nickname, s.rank, s.tied, a.scoreHundredths, s.eta, s.deliverySequence
        )
        from ResultSnapshot s, Participant p, Assessment a
        where p.id = s.userId and a.id = s.assessmentId
          and s.computedAt = :computedAt and s.deliverySequence > :afterSequence
        order by s.deliverySequence asc
        limit :limit
        """,
    )
    fun findRankingRows(
        @Param("computedAt") computedAt: Instant,
        @Param("afterSequence") afterSequence: Int,
        @Param("limit") limit: Int,
    ): List<RankingRow>
}
