package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 사용량을 받지 못한 호출을 비용 합계에서 빼는지 확인한다.
 */
class ModelUsageTest {
    @Test
    fun `사용량을 받지 못한 호출은 비용에 더하지 않는다`() {
        assertFalse(ModelUsage.NONE.isMeasured())
        assertTrue(ModelUsage(1, 0, "").isMeasured())
    }
}
