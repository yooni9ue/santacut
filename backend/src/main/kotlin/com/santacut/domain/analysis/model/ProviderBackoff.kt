package com.santacut.domain.analysis.model

import java.time.Duration
import java.time.Instant

/**
 * AI 제공자 쪽 문제로 작업 잡기를 잠시 멈추는 상태. 앱 메모리에만 두며, 앱이 다시 뜨면 처음부터 센다.
 * 동시에 채점하는 작업자들이 함께 쓰므로 공개 연산을 잠근다.
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

    @Synchronized
    fun isPausedAt(now: Instant): Boolean = pausedUntil?.isAfter(now) == true

    /** 실패 종류에 따라 바로 멈추거나, 연속 실패로 세어 [maxConsecutiveFailures]번째에 멈춘다. */
    @Synchronized
    fun record(failure: ScoringFailure, now: Instant) {
        if (failure.pausesAtOnce) pauseFrom(now)
        if (failure.countsTowardPause) countFailure(now)
    }

    @Synchronized
    fun recordSuccess() {
        consecutiveFailures = 0
    }

    private fun countFailure(now: Instant) {
        consecutiveFailures++
        if (consecutiveFailures >= maxConsecutiveFailures) pauseFrom(now)
    }

    private fun pauseFrom(now: Instant) {
        pausedUntil = now.plus(pause)
        consecutiveFailures = 0
    }
}
