package com.santacut.domain.dossier.model

import com.santacut.TestDossiers
import com.santacut.TestDossiers.partner
import com.santacut.TestDossiers.self
import com.santacut.TestDossiers.selves
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 극값 지표 고르기: 50에서 먼 순, 같은 계열 하나, 최소 메시지 수, 1:1 답장, 카드 금지. */
class MetricReadingsTest {
    @Test
    fun `백분위가 50에서 가장 먼 지표부터 같은 계열은 하나만 고른다`() {
        val stats = TestDossiers.tally(selves(List(40) { "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ 아주 긴 말풍선을 보냅니다" })).stats

        val extremes = TestDossiers.fixture.metrics.readings(stats).extremes(3)

        val keys = listOf(0, 1, 2).mapNotNull { index -> extremes.at(index)?.key }
        assertEquals(3, keys.size)
        assertEquals(keys.size, keys.map { key -> DossierMetric.ofKey(key)?.family }.distinct().size)
        assertTrue("laugh_density" in keys || "laugh_max_run" in keys)
    }

    @Test
    fun `최소 메시지 수에 못 미치는 지표와 1대1이 아닌 대화의 답장 지표는 후보가 아니다`() {
        val few = TestDossiers.fixture.metrics.readings(TestDossiers.tally(selves(List(10) { "ㅋㅋ" })).stats)

        assertNull(few.find("laugh_density"))
        assertTrue(few.find("message_count") != null)
        assertNull(TestDossiers.fixture.metrics.readings(TestDossiers.tally(selves(List(40) { "응" })).stats).find("reply_median_minutes"))
    }

    @Test
    fun `값이 0인 지표는 극값 후보에서 빼고, 삐 처리가 적은 쪽은 카드에 실을 수 있다`() {
        val readings = TestDossiers.fixture.metrics.readings(TestDossiers.tally(selves(List(40) { "응 그래" })).stats)

        val bleep = assertNotNull(readings.find("bleep_density"))
        assertTrue(bleep.isZero())
        assertTrue(bleep.isShareable())
        assertNull(readings.extremes(3).find("bleep_density"))
        assertNull(readings.extremes(3).find("laugh_density"))
    }

    @Test
    fun `느린 답장은 카드에 싣지 않고 빠른 답장은 싣는다`() {
        val slow = readingOfReply(gapMinutes = 300)
        val fast = readingOfReply(gapMinutes = 11)

        assertTrue(slow.isHigh())
        assertFalse(slow.isShareable())
        assertEquals("reply_median_minutes", slow.directionKey())
        assertTrue(fast.isShareable())
        assertEquals("reply_median_minutes_low", fast.directionKey())
    }

    private fun readingOfReply(gapMinutes: Long): MetricReading {
        val messages = List(40) { index ->
            val asked = TestDossiers.START.plusHours(index * 12L)
            listOf(partner(asked, "물어봄"), self(asked.plusMinutes(gapMinutes), "답함"))
        }.flatten()
        return TestDossiers.fixture.metrics.readings(TestDossiers.tally(messages).stats).find("reply_median_minutes")
            ?: error("답장 지표가 없다")
    }
}
