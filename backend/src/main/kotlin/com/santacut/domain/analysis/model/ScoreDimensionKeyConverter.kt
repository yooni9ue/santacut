package com.santacut.domain.analysis.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 엔티티의 [ScoreDimension] 필드를 항목 키(`care_empathy` 등) TEXT 컬럼으로 읽고 쓴다.
 * 모르는 키는 null로 읽는다.
 */
@Converter
class ScoreDimensionKeyConverter : AttributeConverter<ScoreDimension, String> {
    override fun convertToDatabaseColumn(attribute: ScoreDimension?): String? = attribute?.key

    override fun convertToEntityAttribute(dbData: String?): ScoreDimension? = ScoreDimension.ofKey(dbData)
}
