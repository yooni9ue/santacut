package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 순위표 한 페이지와 그 순위 계산의 메타 정보.
 *
 * @property participantCount 가장 최근 순위 계산에 오른 참가자 수. 순위가 없으면 0이다.
 * @property computedAt 가장 최근 순위 계산 시각. 순위가 없으면 null이다.
 * @property nextCursor 다음 페이지 위치. 마지막 페이지면 null이다.
 */
class RankingBoard(
    val seasonYear: Int,
    val participantCount: Long,
    val computedAt: Instant?,
    val stage: RankingStage,
    val rows: RankingRows,
    val nextCursor: RankingCursor?,
) {
    companion object {
        /** 한 줄 더 읽은 [fetched]에서 한 페이지만 남기고 다음 cursor를 정한다. */
        fun of(
            season: Season,
            now: Instant,
            participantCount: Long,
            computedAt: Instant,
            fetched: RankingRows,
            cursor: RankingCursor,
        ): RankingBoard = RankingBoard(
            season.year, participantCount, computedAt, season.stageAt(now),
            fetched.limitTo(cursor.pageSize), fetched.nextCursorAfter(cursor.pageSize, computedAt),
        )

        /** 아직 순위를 계산한 적이 없는 시즌. */
        fun empty(season: Season, now: Instant): RankingBoard =
            RankingBoard(season.year, 0, null, season.stageAt(now), RankingRows.empty(), null)
    }
}
