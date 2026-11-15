package com.santacut.domain.global.model

import java.time.ZoneId

/**
 * 시즌 마감, 배송 출발, AI 일일 예산은 모두 서울 시각을 기준으로 한다.
 */
object SeoulTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
}
