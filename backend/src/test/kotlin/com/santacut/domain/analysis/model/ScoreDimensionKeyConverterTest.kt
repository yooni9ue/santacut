package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoreDimensionKeyConverterTest {
    private val converter = ScoreDimensionKeyConverter()

    @Test
    fun `항목을 키로 저장하고 키를 항목으로 읽으며 모르는 키는 null이다`() {
        assertEquals("cooperation_sharing", converter.convertToDatabaseColumn(ScoreDimension.COOPERATION_SHARING))
        assertEquals(ScoreDimension.CARE_EMPATHY, converter.convertToEntityAttribute("care_empathy"))
        assertNull(converter.convertToEntityAttribute("unknown"))
        assertNull(converter.convertToDatabaseColumn(null))
    }
}
