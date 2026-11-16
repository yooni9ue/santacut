package com.santacut.domain.analysis.model

import java.time.Duration
import java.time.Instant

/**
 * AI 제공자 쪽 문제로 작업 잡기를 잠시 멈추는 상태. 앱 메모리에만 두며, 앱이 다시 뜨면 처음부터 센다.
 *
 * @property pause 멈추는 시간
 * @property maxConsecutiveFailures 이만큼 연달아 실패하면 멈춘다
 */
class ProviderBackoff(
    private val pause: Duration,
    private val maxConsecutiveFailures: Int,
) {
    private var pausedUntil: Instant? = null
    private var consecutiveFailures = 0

    fun isPausedAt(now: Instant): Boolean = pausedUntil?.isAfter(now) == true

    /** 제공자가 호출을 막았다. 바로 멈춘다. */
    fun pauseFrom(now: Instant) {
        pausedUntil = now.plus(pause)
        consecutiveFailures = 0
    }

    /** 채점이 실패했다. [maxConsecutiveFailures]번 연달아 실패하면 멈춘다. */
    fun recordFailure(now: Instant) {
        consecutiveFailures++
        if (consecutiveFailures >= maxConsecutiveFailures) pauseFrom(now)
    }

    fun recordSuccess() {
        consecutiveFailures = 0
    }
}
