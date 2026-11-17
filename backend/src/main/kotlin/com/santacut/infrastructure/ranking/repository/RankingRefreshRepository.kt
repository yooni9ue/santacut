package com.santacut.infrastructure.ranking.repository

import com.santacut.domain.ranking.model.RankingRefresh
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RankingRefreshRepository : JpaRepository<RankingRefresh, Int> {
    /** 다시 계산 요청 판을 하나 올린다. 바뀐 행 수를 돌려준다(행이 없으면 0). */
    @Modifying(flushAutomatically = true)
    @Query("update RankingRefresh r set r.requestedVersion = r.requestedVersion + 1 where r.id = :id")
    fun markRequested(@Param("id") id: Int): Int

    /** [version] 판까지 순위에 반영했다고 표시한다. 이미 더 뒤 판까지 반영했으면 그대로 둔다. */
    @Modifying(flushAutomatically = true)
    @Query("update RankingRefresh r set r.rebuiltVersion = :version where r.id = :id and r.rebuiltVersion < :version")
    fun markRebuilt(@Param("id") id: Int, @Param("version") version: Long): Int
}
