package com.santacut

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 테스트가 원하는 시각으로 옮길 수 있는 UTC 시계. 백그라운드 스레드에서도 읽으므로 값은 volatile로 둔다.
 */
class MutableClock(initial: Instant) : Clock() {
    @Volatile
    private var current: Instant = initial

    fun moveTo(instant: Instant) {
        current = instant
    }

    override fun instant(): Instant = current

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)
}
