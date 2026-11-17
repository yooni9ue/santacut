package com.santacut.domain.ranking.model

import java.time.Duration
import java.time.Instant

/**
 * 순위표 첫 페이지를 잠깐 들고 있는 캐시. 앱 메모리에 한 장만 둔다.
 * 같은 순위 계산([RankingBoard.computedAt])과 같은 확정 상태일 때만 쓰므로 다시 계산하면 저절로 버려진다.
 * 요청 스레드들이 함께 쓰므로 공개 연산을 잠근다.
 *
 * @property ttl 한 번 읽은 첫 페이지를 다시 쓰는 시간
 */
class RankingBoardCache(private val ttl: Duration) {
    private var cached: RankingBoard? = null
    private var expiresAt: Instant = Instant.MIN

    /** [computedAt] 순위 계산의 [stage] 상태 첫 페이지가 아직 살아 있으면 돌려준다. */
    @Synchronized
    fun find(computedAt: Instant, stage: RankingStage, now: Instant): RankingBoard? =
        cached?.takeIf { board -> now.isBefore(expiresAt) && board.computedAt == computedAt && board.stage == stage }

    @Synchronized
    fun store(board: RankingBoard, now: Instant) {
        cached = board
        expiresAt = now.plus(ttl)
    }

    /** 순위를 다시 계산했으니 들고 있던 첫 페이지를 버린다. */
    @Synchronized
    fun invalidate() {
        cached = null
    }
}
