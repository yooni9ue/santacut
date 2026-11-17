package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 원본 파일 접수 동시 처리 상한.
 *
 * @property maxConcurrentUploads 동시에 메모리로 읽어 저장하는 원본 파일 수
 * @property retryAfter 상한에 걸린 요청에 Retry-After로 알릴 대기 시간
 */
@ConfigurationProperties(prefix = "santacut.upload-intake")
data class UploadIntakeProperties(
    val maxConcurrentUploads: Int = 2,
    val retryAfter: Duration = Duration.ofSeconds(10),
)
