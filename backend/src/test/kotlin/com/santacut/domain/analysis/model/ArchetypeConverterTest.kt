package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArchetypeConverterTest {
    private val converter = ArchetypeConverter()

    @Test
    fun `유형을 상수 이름으로 저장하고 읽으며 지금은 없는 이름은 null이다`() {
        assertEquals("WORKSHOP_ELF", converter.convertToDatabaseColumn(Archetype.WORKSHOP_ELF))
        assertEquals(Archetype.CAROL_LEADER, converter.convertToEntityAttribute("CAROL_LEADER"))
        assertNull(converter.convertToEntityAttribute("RETIRED_ARCHETYPE"))
        assertNull(converter.convertToDatabaseColumn(null))
    }
}
