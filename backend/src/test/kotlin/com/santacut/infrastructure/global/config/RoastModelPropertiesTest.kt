package com.santacut.infrastructure.global.config

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 작가 모델 설정 검사. 틀린 값이면 앱이 뜰 때 막는다. */
class RoastModelPropertiesTest {
    @Test
    fun `다시 쓰기 횟수가 음수면 설정을 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { RoastModelProperties(maxRewrites = -1) }
    }

    @Test
    fun `모르는 생각 수준이면 설정을 만들 수 없고 비우거나 아는 값이면 된다`() {
        assertFailsWith<IllegalArgumentException> { RoastModelProperties(thinkingLevel = "LOWEST") }
        assertEquals(1, RoastModelProperties(thinkingLevel = "", maxRewrites = 0).maxCalls())
        assertEquals("HIGH", RoastModelProperties(thinkingLevel = "HIGH").thinkingLevel)
    }

    @Test
    fun `작가 단가 기본값은 Gemini 3_5 Flash 정가이고 생각 토큰은 출력 단가로 센다`() {
        val properties = RoastModelProperties()

        assertEquals(0, BigDecimal("1.5").compareTo(properties.inputUsdPerMillionTokens))
        assertEquals(0, BigDecimal("0.15").compareTo(properties.cachedInputUsdPerMillionTokens))
        assertEquals(0, BigDecimal("9").compareTo(properties.outputUsdPerMillionTokens))
        assertEquals(properties.outputUsdPerMillionTokens, properties.thinkingUsdPerMillionTokens)
    }
}
