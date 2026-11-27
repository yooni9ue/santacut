package com.santacut.domain.roast.model

import com.santacut.TestRecapFacts.COAL
import com.santacut.TestRecapFacts.PUSHER_LINES
import com.santacut.TestRecapFacts.ROASTER_LINES
import com.santacut.TestRecapFacts.briefOf
import com.santacut.TestRecapFacts.metric
import com.santacut.TestRecapFacts.metricOrNull
import com.santacut.TestRecapFacts.pusherFacts
import com.santacut.TestRecapFacts.roasterFacts
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 리캡 5차 심사 지적 2·8: 말풍선 양 칩이 깎인 말로 뒤집히던 문제, ㅋ 밀도 '3번에 1번'에 노웃음상이 붙던 문제,
 * 깎인 말과 같은 버릇(또는 깎인 버릇 자체)을 살아남은 말로 띄우던 문제.
 */
class RecapChipDirectionTest {
    private val catalog = TestRoasts.catalog
    private val rules = catalog.rules

    @Test
    fun `말풍선 양 칩은 적어도 깎인 말이 아니라 중립이고 많으면 좋은 쪽이다`() {
        listOf("message_count" to 500.0, "messages_per_day" to 2.2, "active_days" to 40.0).forEach { (key, value) ->
            assertEquals(ChipDirection.NEUTRAL, metric(key, value, VERY_LOW).direction, key)
            assertEquals(ChipDirection.NEUTRAL, metric(key, value, LOW).direction, key)
            assertEquals(ChipDirection.GOOD, metric(key, value, VERY_HIGH).direction, key)
        }
    }

    @Test
    fun `방향 표 감사 - 양·시간대·길이 계열은 적다는 쪽을 깎인 말로 쓰지 않는다`() {
        val lowIsNeverBad = setOf("volume", "time", "shape", "week", "spike", "media", "emoji", "burst", "mark")
        catalog.facts.toList().filter { spec -> spec.group in lowIsNeverBad }.forEach { spec ->
            assertFalse(spec.directions.take(LOW_LEVELS).contains(ChipDirection.BAD), "${spec.key} ${spec.directions}")
        }
    }

    @Test
    fun `회귀 - ㅋ 밀도는 열 번에 한 번보다 드물고 훨씬 낮은 수준일 때만 깎인 말이다`() {
        assertEquals(ChipDirection.BAD, metric("laugh_density", ONE_IN_ELEVEN, VERY_LOW).direction)
        assertEquals(ChipDirection.NEUTRAL, metric("laugh_density", ONE_IN_THREE, VERY_LOW).direction)
        assertEquals(ChipDirection.NEUTRAL, metric("laugh_density", ONE_IN_THREE, LOW).direction)
        assertEquals(ChipDirection.NEUTRAL, metric("laugh_density", ONE_IN_ELEVEN, LOW).direction)
        val card = EvidenceDraft("cut", null, "laugh_density", "노웃음상", "방에서 웃지 않고 말을 끝낸다.")
        val roaster = briefOf(COAL, roasterFacts(), ROASTER_LINES)
        assertTrue(card.problems(roaster, rules).any { reason -> reason.startsWith("이 칩(laugh_density)은 cut에 쓰지 않는다") })
    }

    @Test
    fun `cut_max_value는 나쁜 쪽 방향이 있는 지표에만 쓴다`() {
        val spec = assertNotNull(catalog.facts.find("emoji_density")).copy(cutMaxValue = 1.0)

        assertTrue(spec.violations().any { reason -> reason.contains("cut_max_value") })
        assertEquals(null, metricOrNull("laugh_density", ONE_IN_THREE, MIDDLE))
    }

    @Test
    fun `회귀 - 따뜻한 말 칩을 살아남은 말로 쓰면 고마워 0번 깎인 말과 같은 버릇이라 떨어진다`() {
        val roaster = briefOf(COAL, roasterFacts(), ROASTER_LINES)
        val cards = EvidenceDrafts(
            listOf(
                EvidenceDraft("cut", null, ThanksFactSpec.KEY, "감사실종상", "고맙다는 말이 실종됐다."),
                EvidenceDraft("keep", 4, "kind_words_density", "칭찬투척상", "잘했다는 칭찬은 툭 던진다."),
            ),
        )

        val checks = cards.checks(roaster, rules)

        assertTrue(checks[1].reasons.any { reason -> reason.startsWith("깎인 말과 이유·살아남은 말이 같은 버릇(kind)") }, checks[1].reasons.toString())
        assertEquals(roaster.topicOf(ThanksFactSpec.KEY), roaster.topicOf("kind_words_density"))
    }

    @Test
    fun `입버릇은 입버릇마다 다른 버릇이라 다른 입버릇끼리는 깎인 말과 살아남은 말에 함께 쓸 수 있다`() {
        val roaster = briefOf(COAL, roasterFacts(), ROASTER_LINES)
        val cards = EvidenceDrafts(
            listOf(
                EvidenceDraft("cut", 1, PhraseFactSpec.KEY_PREFIX + "mwoim", "옷평가상", "옷 보자마자 뭐임부터 날린다."),
                EvidenceDraft("keep", 3, PhraseFactSpec.KEY_PREFIX + "chukhahae", "축하배달상", "축하는 꼬박꼬박 챙겨 준다."),
            ),
        )

        assertFalse(cards.checks(roaster, rules)[1].reasons.any { reason -> reason.contains("같은 버릇") })
    }

    @Test
    fun `회귀 - pusher 대체 살아남은 말은 깎인 버릇인 했어를 띄우지 않는다`() {
        val pusher = briefOf(COAL, pusherFacts(), PUSHER_LINES)

        val keeps = pusher.fallbackEvidence(catalog.fallback).toList().filter { card -> card.kind == EvidenceKind.KEEP }

        assertFalse(keeps.any { card -> card.chipKey == PhraseFactSpec.KEY_PREFIX + "haeteo_q" }, keeps.toString())
        assertTrue(keeps.all { card -> pusher.directionOf(card.chipKey) == ChipDirection.GOOD })
    }

    @Test
    fun `회귀 - roaster 대체 살아남은 말은 고마워 0번과 같은 버릇인 따뜻한 말을 띄우지 않는다`() {
        val roaster = briefOf(COAL, roasterFacts(), ROASTER_LINES)

        val cards = roaster.fallbackEvidence(catalog.fallback).toList()

        assertTrue(cards.any { card -> card.chipKey == ThanksFactSpec.KEY }, cards.toString())
        assertEquals(listOf(PhraseFactSpec.KEY_PREFIX + "chukhahae"), cards.filter { card -> card.kind == EvidenceKind.KEEP }.map(RoastEvidence::chipKey))
    }

    companion object {
        private const val VERY_LOW = 5.0
        private const val LOW = 25.0
        private const val MIDDLE = 50.0
        private const val VERY_HIGH = 95.0
        private const val LOW_LEVELS = 2
        private const val ONE_IN_ELEVEN = 9.0
        private const val ONE_IN_THREE = 33.0
    }
}
