package com.santacut.infrastructure.ranking.repository

import com.santacut.domain.ranking.model.RankedAssessmentTime
import com.santacut.domain.ranking.model.RankingRow
import com.santacut.domain.ranking.model.ResultSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ResultSnapshotRepository : JpaRepository<ResultSnapshot, String> {
    fun findFirstByUserIdOrderByComputedAtDesc(userId: String): ResultSnapshot?

    fun findFirstByOrderByComputedAtDesc(): ResultSnapshot?

    /** 참가자의 최신 순위 스냅숏이 담은 평가를 만든 시각. 스냅숏과 평가를 한 번에 읽는다. 순위에 오른 적이 없으면 null이다. */
    @Query(
        """
        select a.createdAt from ResultSnapshot s join Assessment a on a.id = s.assessmentId
        where s.userId = :userId
        order by s.computedAt desc
        limit 1
        """,
    )
    fun findRankedAssessmentTime(@Param("userId") userId: String): Instant?

    /** [userIds] 참가자마다 최신 순위 스냅숏이 담은 평가를 만든 시각. 순위에 오른 적이 없는 참가자는 빠진다. */
    @Query(
        """
        select new com.santacut.domain.ranking.model.RankedAssessmentTime(s.userId, a.createdAt)
        from ResultSnapshot s join Assessment a on a.id = s.assessmentId
        where s.userId in :userIds
          and s.computedAt = (select max(latest.computedAt) from ResultSnapshot latest where latest.userId = s.userId)
        """,
    )
    fun findRankedAssessmentTimes(@Param("userIds") userIds: Collection<String>): List<RankedAssessmentTime>

    fun countByComputedAt(computedAt: Instant): Long

    /** 공유 링크가 붙잡지 않은 스냅숏을 한 문장으로 지운다. 지운 행 수를 돌려준다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ResultSnapshot s where s.id not in (select sh.snapshotId from Share sh)")
    fun deleteUnshared(): Int

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
