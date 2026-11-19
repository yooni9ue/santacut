package com.santacut.domain.dossier.model

import com.santacut.domain.global.model.SeoulTime
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 조서 문구에 들어가는 숫자를 서버가 단위까지 붙여 쓰는 규칙. 템플릿에는 숫자를 직접 쓰지 않고 이 값만 슬롯으로 넣는다.
 */
object DossierFormat {
    private const val TIMES_UNIT = "번"
    private const val RANK_UNIT = "위"
    private const val NATIONAL_PREFIX = "전국 "
    private const val PERCENT_UNIT = "%"
    private const val MONTH_UNIT = "월"
    private const val MINUTES_PER_HOUR = 60
    private const val MINUTE_BAND = 10
    private const val NOON = 12
    private const val DAWN_END = 5
    private const val EVENING_START = 18
    private const val NIGHT_START = 21
    private val DIGIT = Regex("[0-9]")
    private const val MASK = "█"
    private const val TENTHS = 10L
    private const val ONE_DECIMAL = "%,.1f"

    /** 1,234번 */
    fun times(count: Int): String = grouped(count.toLong()) + TIMES_UNIT

    /** 3,412위 */
    fun rank(rank: Int): String = grouped(rank.toLong()) + RANK_UNIT

    /** 전국 3,412위 */
    fun nationalRank(rank: Int): String = NATIONAL_PREFIX + rank(rank)

    /** 반올림한 정수에 천 단위 쉼표와 [unit]을 붙인다. 212개, 1,234자 */
    fun amount(value: Double, unit: String): String = grouped(value.roundToLong()) + unit

    /** 소수 첫째 자리까지, 천 단위 쉼표를 붙인다. 끝이 .0이면 떼어 낸다. 3.8개, 7배, 1,234.5자 */
    fun decimal(value: Double, unit: String): String {
        val tenths = (value * TENTHS).roundToLong()
        val text = if (tenths % TENTHS == 0L) grouped(tenths / TENTHS) else String.format(Locale.ROOT, ONE_DECIMAL, tenths / TENTHS.toDouble())
        return text + unit
    }

    /** 반올림한 정수 %. 23% */
    fun percent(value: Double): String = value.roundToInt().toString() + PERCENT_UNIT

    /** 분을 10분 단위 시간대로 뭉갠다. 7분, 40분대, 2시간대, 2시간 40분대 */
    fun minutes(minutes: Int): String {
        if (minutes < MINUTE_BAND) return "${minutes}분"
        if (minutes < MINUTES_PER_HOUR) return "${minutes / MINUTE_BAND * MINUTE_BAND}분대"
        val band = minutes % MINUTES_PER_HOUR / MINUTE_BAND * MINUTE_BAND
        val hours = "${minutes / MINUTES_PER_HOUR}시간"
        return if (band == 0) "${hours}대" else "$hours ${band}분대"
    }

    /** 시(0~23)를 시간대 이름으로. 밤 12시대, 새벽 1시대, 오전 9시대, 낮 12시대, 오후 3시대, 저녁 7시대, 밤 11시대 */
    fun hour(hour: Int): String = when {
        hour == 0 -> "밤 ${NOON}시대"
        hour <= DAWN_END -> "새벽 ${hour}시대"
        hour < NOON -> "오전 ${hour}시대"
        hour == NOON -> "낮 ${NOON}시대"
        hour < EVENING_START -> "오후 ${hour - NOON}시대"
        hour < NIGHT_START -> "저녁 ${hour - NOON}시대"
        else -> "밤 ${hour - NOON}시대"
    }

    /** 7월 */
    fun month(month: Int): String = month.toString() + MONTH_UNIT

    /** 숫자를 가린다. 티저에 쓴다. ██,███개 */
    fun masked(text: String): String = text.replace(DIGIT, MASK)

    /** 배송 도착 시각(한국 시각). 12월 25일 오전 6시 12분 */
    fun eta(eta: Instant): String {
        val local = eta.atZone(SeoulTime.ZONE)
        val period = if (local.hour < NOON) "오전" else "오후"
        val hour = if (local.hour % NOON == 0) NOON else local.hour % NOON
        return "${local.monthValue}월 ${local.dayOfMonth}일 $period ${hour}시 ${local.minute}분"
    }

    private fun grouped(value: Long): String = String.format(Locale.ROOT, "%,d", value)
}
