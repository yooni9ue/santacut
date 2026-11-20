package com.santacut.domain.global.config

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 순위 계산 설정 검사. 틀린 값이면 앱이 뜰 때 막는다. */
class RankingPropertiesTest {
    @Test
    fun `커밋 뒤 계산의 기본값은 모으기 1초, 최소 간격 5초다`() {
        val properties = RankingProperties()

        assertEquals(Duration.ofSeconds(1), properties.rebuildDebounce)
        assertEquals(Duration.ofSeconds(5), properties.rebuildMinInterval)
    }

    @Test
    fun `모으기 시간이나 최소 간격이 음수면 설정을 만들 수 없고 0이면 된다`() {
        assertFailsWith<IllegalArgumentException> { RankingProperties(rebuildDebounce = Duration.ofMillis(-1)) }
        assertFailsWith<IllegalArgumentException> { RankingProperties(rebuildMinInterval = Duration.ofMillis(-1)) }
        assertEquals(Duration.ZERO, RankingProperties(rebuildDebounce = Duration.ZERO, rebuildMinInterval = Duration.ZERO).rebuildMinInterval)
    }
}
