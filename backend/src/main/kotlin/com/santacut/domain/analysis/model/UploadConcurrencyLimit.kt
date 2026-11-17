package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Semaphore

/**
 * 동시에 메모리로 읽어 저장하는 원본 파일 수. 원본은 최대 20MB라 256MB 힙을 지키려고 앱 전체 상한을 둔다.
 * 자리가 없으면 기다리지 않고 [ErrorCode.UPLOAD_BUSY](503)로 바로 돌려보낸다.
 *
 * @property retryDelay 자리가 없을 때 다시 올려 볼 수 있다고 알릴 대기 시간
 */
class UploadConcurrencyLimit(
    maxConcurrentUploads: Int,
    private val retryDelay: Duration,
) {
    private val slots = Semaphore(maxConcurrentUploads)

    /** 자리 하나를 잡고 [action]을 한 뒤 자리를 돌려준다. */
    fun <T> runWithinLimit(now: Instant, action: () -> T): T {
        if (!slots.tryAcquire()) throw SantaCutException(ErrorCode.UPLOAD_BUSY, retryAt = now.plus(retryDelay))
        try {
            return action()
        } finally {
            slots.release()
        }
    }
}
