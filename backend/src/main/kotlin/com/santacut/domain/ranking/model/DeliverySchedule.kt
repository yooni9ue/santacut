package com.santacut.domain.ranking.model

import java.time.ZonedDateTime

/**
 * 배송 기간에 맞춘 배송 일정.
 * 누적 작업 시간이 배송 기간([Delivery.workSecondsFor])을 넘으면 모든 작업 시간을 같은 비율로 줄인다.
 * 참가자가 많을수록 썰매가 빨라지고 정거장에 머무는 시간도 줄어든다고 본다. 배송 순서는 그대로이고 같은 입력이면 늘 같은 결과다.
 *
 * @property scale 작업 시간에 곱하는 비율. 기간 안에 들어오면 1이다.
 * @property capacitySeconds 배송 기간 전체의 작업 시간(초). 부동소수 오차로 기간을 넘지 않게 묶는다.
 */
class DeliverySchedule private constructor(
    private val start: ZonedDateTime,
    private val scale: Double,
    private val capacitySeconds: Double,
) {
    /** 출발부터 [workSeconds]만큼 일한 뒤의 도착 시각. */
    fun arrival(workSeconds: Double): ZonedDateTime =
        Delivery.arrival(start, (workSeconds * scale).coerceAtMost(capacitySeconds))

    /** 참가자가 많아 작업 시간을 줄였는지. */
    fun isCompressed(): Boolean = scale < NO_SCALE

    companion object {
        private const val NO_SCALE = 1.0

        /** 마지막 참가자까지의 누적 작업 시간 [totalWorkSeconds]가 [activeDays]일 안에 끝나도록 비율을 정한다. */
        fun fit(start: ZonedDateTime, totalWorkSeconds: Double, activeDays: Int): DeliverySchedule {
            require(activeDays > 0) { "배송 기간은 하루 이상이어야 합니다." }
            val capacity = Delivery.workSecondsFor(activeDays)
            val scale = if (totalWorkSeconds > capacity) capacity / totalWorkSeconds else NO_SCALE
            return DeliverySchedule(start, scale, capacity)
        }
    }
}
