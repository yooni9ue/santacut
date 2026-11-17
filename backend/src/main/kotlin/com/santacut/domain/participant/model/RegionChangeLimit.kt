package com.santacut.domain.participant.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Duration
import java.time.Instant

/**
 * 지역 변경 한도. 지역을 마음대로 바꿔 가며 도착 예정 시각 차이로 다른 참가자의 지역이나 출발지를 좁혀 가지 못하게 한다.
 *
 * @property maxPerWindow [window] 동안 바꿀 수 있는 횟수
 * @property window 짧은 한도를 세는 창 길이
 * @property maxPerSeason 한 시즌 동안 바꿀 수 있는 횟수. 참가자는 한 시즌에만 있으므로 참가자의 모든 변경을 센다.
 */
class RegionChangeLimit(
    private val maxPerWindow: Int,
    private val window: Duration,
    private val maxPerSeason: Int,
) {
    /** 짧은 한도를 셀 기간의 시작 시각. 이 시각 이후 바꾼 것만 센다. */
    fun windowStart(now: Instant): Instant = now.minus(window)

    /**
     * 시즌 한도에 닿았으면 [ErrorCode.REGION_CHANGE_SEASON_LIMIT](다시 시도할 시각 없음),
     * 창 안 횟수가 한도에 닿았으면 [ErrorCode.REGION_CHANGE_LIMITED]다. 창 안 가장 오래된 변경([oldestRecentAt])이
     * 창 밖으로 빠지는 시각을 다시 바꿀 수 있는 시각으로 알린다.
     */
    fun verify(seasonChanges: Long, recentChanges: Long, oldestRecentAt: Instant?) {
        if (seasonChanges >= maxPerSeason) throw SantaCutException(ErrorCode.REGION_CHANGE_SEASON_LIMIT)
        if (recentChanges < maxPerWindow) return
        throw SantaCutException(ErrorCode.REGION_CHANGE_LIMITED, retryAt = oldestRecentAt?.plus(window))
    }
}
