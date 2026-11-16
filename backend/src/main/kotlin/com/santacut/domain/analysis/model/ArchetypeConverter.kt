package com.santacut.domain.analysis.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 엔티티의 [Archetype] 필드를 상수 이름 TEXT 컬럼으로 읽고 쓴다.
 * 모르는 이름(유형 상수를 바꾸기 전에 만든 공유 링크 등)은 오류 대신 null로 읽어 페이지가 그대로 뜨게 한다.
 */
@Converter
class ArchetypeConverter : AttributeConverter<Archetype, String> {
    override fun convertToDatabaseColumn(attribute: Archetype?): String? = attribute?.name

    override fun convertToEntityAttribute(dbData: String?): Archetype? = Archetype.ofName(dbData)
}
