package com.santacut.domain.global.model

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 클라이언트(IP)별 요청 창 목록. 앱 하나(단일 인스턴스)의 메모리에만 둔다.
 */
class RequestWindows(
    private val values: ConcurrentHashMap<String, RequestWindow> = ConcurrentHashMap(),
) {
    /** [client]의 요청을 하나 세고 센 뒤의 창을 돌려준다. 여러 스레드가 함께 불러도 한 번씩만 센다. */
    fun record(client: String, now: Instant, length: Duration): RequestWindow =
        values.compute(client) { _, current -> current?.next(now, length) ?: RequestWindow.startingAt(now) }
            ?: RequestWindow.startingAt(now)

    /** 끝난 창을 지운다. 지난 IP가 메모리에 쌓이지 않게 한다. */
    fun evictOverAt(now: Instant, length: Duration) {
        values.values.removeIf { it.isOverAt(now, length) }
    }

    fun size(): Int = values.size
}
