package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Duration
import java.time.Instant

/**
 * 동시에 열어 둘 수 있는 진행 상황 스트림 수. 512MB 서버를 지키려고 앱 전체 상한을 둔다.
 * 참가자 한 명은 스트림을 하나만 쓰며, 새로 열면 이전 스트림을 대신하므로 상한에 걸리지 않는다.
 *
 * @property maxOpenStreams 앱 전체에서 동시에 열 수 있는 스트림 수
 * @property retryDelay 상한에 걸렸을 때 다시 열어 볼 수 있다고 알릴 대기 시간
 */
class AnalysisStreamLimit(
    private val maxOpenStreams: Int,
    private val retryDelay: Duration,
) {
    /** 상한에 닿았고 이 참가자의 스트림을 대신하는 것도 아니면 [ErrorCode.ANALYSIS_STREAM_LIMIT]다. 화면은 폴링으로 바꾼다. */
    fun verifyOpenable(openStreams: Int, replacing: Boolean, now: Instant) {
        if (replacing || openStreams < maxOpenStreams) return
        throw SantaCutException(ErrorCode.ANALYSIS_STREAM_LIMIT, "open=$openStreams", now.plus(retryDelay))
    }
}
