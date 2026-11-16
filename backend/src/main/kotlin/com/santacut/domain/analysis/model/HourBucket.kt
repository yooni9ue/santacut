package com.santacut.domain.analysis.model

/**
 * 메시지를 보낸 시각(한국 시각)의 6시간 구간. 0~6시, 6~12시, 12~18시, 18~24시 순서다.
 */
enum class HourBucket {
    NIGHT,
    MORNING,
    AFTERNOON,
    EVENING,
    ;

    companion object {
        private const val HOURS_PER_BUCKET = 6

        /** 0~23시를 구간으로 바꾼다. */
        fun of(hour: Int): HourBucket = entries[hour / HOURS_PER_BUCKET]
    }
}
