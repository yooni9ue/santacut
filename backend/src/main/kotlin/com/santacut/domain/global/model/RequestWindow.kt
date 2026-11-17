package com.santacut.domain.global.model

import java.time.Duration
import java.time.Instant

/**
 * 한 클라이언트가 고정 시간 창 안에 보낸 요청 수.
 *
 * @property startedAt 이 창이 시작된 시각
 * @property count 이 창에서 센 요청 수
 */
data class RequestWindow(
    val startedAt: Instant,
    val count: Int,
) {
    /** [now]의 요청을 하나 더 센 창. 창이 끝났으면 [now]부터 새 창을 연다. */
    fun next(now: Instant, length: Duration): RequestWindow =
        if (isOverAt(now, length)) startingAt(now) else copy(count = count + 1)

    /** 창이 끝나 새 요청을 받을 수 있는 시각. */
    fun retryAt(length: Duration): Instant = startedAt.plus(length)

    fun isOverAt(now: Instant, length: Duration): Boolean = !now.isBefore(retryAt(length))

    companion object {
        fun startingAt(now: Instant): RequestWindow = RequestWindow(now, 1)
    }
}
