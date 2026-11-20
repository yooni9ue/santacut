package com.santacut.domain.dossier.model

import com.santacut.TestDossiers
import com.santacut.TestRoastStats
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 이미 저장한 집계(요일·시간대 칸, 최다일, 글 메시지 수)에서 끌어낸 지표. 다시 세지 않아 옛 조서에도 쓴다. */
class DerivedMetricsTest {
    private val heavy = TestRoastStats.heavyUser

    @Test
    fun `요일·시간대 칸에서 주말, 아침, 근무시간, 시간 집중도 비율을 낸다`() {
        assertEquals(0.236, DossierMetric.WEEKEND_RATIO.valueIn(heavy))
        assertEquals(0.047, DossierMetric.MORNING_RATIO.valueIn(heavy))
        assertEquals(0.432, DossierMetric.OFFICE_HOURS_RATIO.valueIn(heavy))
        assertEquals(0.124, DossierMetric.PEAK_HOUR_SHARE.valueIn(heavy))
    }

    @Test
    fun `양 집계에서 최다일 배수, 사진·이모티콘 비율, 활동일당 말풍선을 낸다`() {
        assertEquals(5.3, DossierMetric.BUSIEST_DAY_MULTIPLE.valueIn(heavy))
        assertEquals(0.06, DossierMetric.MEDIA_RATIO.valueIn(heavy))
        assertEquals(75.8, DossierMetric.MESSAGES_PER_DAY.valueIn(heavy))
    }

    @Test
    fun `칸이 비었거나 메시지가 없으면 값이 없다`() {
        val empty = heavy.copy(
            clock = heavy.clock.copy(hourly = emptyList(), weekday = emptyList(), peakHour = null),
            volume = VolumeStats(0, 0, 0, 0, null),
        )

        listOf(
            DossierMetric.WEEKEND_RATIO, DossierMetric.MORNING_RATIO, DossierMetric.OFFICE_HOURS_RATIO, DossierMetric.PEAK_HOUR_SHARE,
            DossierMetric.BUSIEST_DAY_MULTIPLE, DossierMetric.MEDIA_RATIO, DossierMetric.MESSAGES_PER_DAY,
        ).forEach { metric -> assertNull(metric.valueIn(empty), metric.key) }
    }

    @Test
    fun `새 지표는 실제 사전에 기준값과 극값 문구가 있다`() {
        val real = TestDossiers.readReal()

        listOf("weekend_ratio", "morning_ratio", "office_hours_ratio", "peak_hour_share", "busiest_day_multiple", "media_ratio", "messages_per_day")
            .forEach { key -> assertEquals(true, real.metrics.toList().any { spec -> spec.key == key } && real.copy.extremes.has(key), key) }
    }
}
