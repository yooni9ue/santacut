package com.santacut.domain.global.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Duration
import java.time.Instant

/**
 * 클라이언트 IP 하나가 [window] 동안 할 수 있는 요청 수. 참가자를 새로 만들어 참가자별 한도를 피하는 일을 막는다.
 * 고정 창으로 센다. 창은 그 IP의 첫 요청 때 열리고 [window]가 지나면 닫힌다.
 *
 * @property maxRequests 창 하나에서 받을 요청 수
 * @property window 창 길이
 */
class ClientRateLimit(
    private val maxRequests: Int,
    private val window: Duration,
) {
    private val windows = RequestWindows()

    /**
     * [client]의 요청을 하나 센다. 한도를 넘으면 [ErrorCode.CLIENT_RATE_LIMITED]이고 retry_at은 창이 닫히는 시각이다.
     * [client]가 null이면(HTTP 밖의 호출) 세지 않는다.
     */
    fun acquire(client: String?, now: Instant) {
        if (client == null) return
        if (windows.size() > MAX_TRACKED_CLIENTS) windows.evictOverAt(now, window)
        val current = windows.record(client, now, window)
        if (current.count <= maxRequests) return
        throw SantaCutException(ErrorCode.CLIENT_RATE_LIMITED, "count=${current.count}", current.retryAt(window))
    }

    companion object {
        /** 이보다 많은 IP를 기억하면 끝난 창부터 지운다. */
        const val MAX_TRACKED_CLIENTS = 10_000
    }
}
