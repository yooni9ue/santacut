package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 근거 부족 항목의 저장 형식과 API 키 목록을 확인한다.
 */
class LowConfidenceDimensionsTest {
    @Test
    fun `저장 값은 항목 키를 쉼표로 잇고 읽을 때는 항목 순서로 돌려준다`() {
        val dimensions = LowConfidenceDimensions(listOf(ScoreDimension.POSITIVITY_SUPPORT, ScoreDimension.COOPERATION_SHARING))

        assertEquals("positivity_support,cooperation_sharing", dimensions.toColumn())
        assertEquals(dimensions, LowConfidenceDimensions.fromColumn("cooperation_sharing,positivity_support"))
    }

    @Test
    fun `값이 없거나 모르는 키는 근거 부족이 아니다`() {
        assertEquals(emptyList(), LowConfidenceDimensions.fromColumn(null).toKeys())
        assertEquals(emptyList(), LowConfidenceDimensions.fromColumn("").toKeys())
        assertEquals(listOf("care_empathy"), LowConfidenceDimensions.fromColumn("care_empathy,unknown").toKeys())
    }

    @Test
    fun `모든 항목이 근거 부족일 때만 전부다`() {
        assertTrue(LowConfidenceDimensions(ScoreDimension.entries.toList()).coversAll())
        assertFalse(LowConfidenceDimensions(listOf(ScoreDimension.CARE_EMPATHY)).coversAll())
    }
}
