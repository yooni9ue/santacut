package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 클라이언트 IP별 요청 한도. 앱 메모리에서 세므로 앱이 다시 뜨면 처음부터 센다.
 *
 * @property registrationsPerClient IP 하나가 [registrationWindow] 동안 만들 수 있는 참가자 수
 * @property registrationWindow 참가 등록 한도를 세는 창 길이
 * @property submissionsPerClient IP 하나가 [submissionWindow] 동안 낼 수 있는 분석 제출 수(JSON 제출과 원본 파일 합산)
 * @property submissionWindow 분석 제출 한도를 세는 창 길이
 */
@ConfigurationProperties(prefix = "santacut.rate-limit")
data class ClientRateLimitProperties(
    val registrationsPerClient: Int = 10,
    val registrationWindow: Duration = Duration.ofHours(1),
    val submissionsPerClient: Int = 20,
    val submissionWindow: Duration = Duration.ofHours(24),
)
