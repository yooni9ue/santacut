package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 본인 메시지 전체에서 서버가 계산한 활동 통계. 채점 프롬프트가 메시지 양과 활동 시간대를 점수에 쓰지 못하게 하므로
 * AI에는 보내지 않고 서버 쪽(사용량 로그 등)에서만 쓴다.
 *
 * @property eventCount 본인 메시지 수
 * @property activeDays 메시지를 보낸 날 수
 * @property messagesPerActiveDay 보낸 날 하루 평균 메시지 수(소수 첫째 자리)
 * @property hourCounts 시간 구간별 메시지 수
 * @property longestStreakDays 하루도 빠지지 않고 메시지를 보낸 가장 긴 날 수
 * @property laughEmojiRatio 웃음·울음·이모지가 든 메시지 비율(소수 둘째 자리)
 */
data class ScoringFeatures(
    val eventCount: Int,
    val activeDays: Int,
    val messagesPerActiveDay: BigDecimal,
    val hourCounts: HourCounts,
    val longestStreakDays: Int,
    val laughEmojiRatio: BigDecimal,
)
