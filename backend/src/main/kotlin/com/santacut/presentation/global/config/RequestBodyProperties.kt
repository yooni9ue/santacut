package com.santacut.presentation.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize

/**
 * 파일 업로드가 아닌 요청 본문의 크기 상한.
 *
 * @property maxSize JSON 같은 일반 요청 본문의 최대 크기. 분석 JSON 제출(최대 2,000개, 200,000자)이 넉넉히 들어가는 값이다
 */
@ConfigurationProperties(prefix = "santacut.request-body")
data class RequestBodyProperties(
    val maxSize: DataSize = DataSize.ofMegabytes(2),
)
