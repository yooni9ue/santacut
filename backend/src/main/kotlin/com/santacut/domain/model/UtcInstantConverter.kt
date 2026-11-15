package com.santacut.domain.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Instant

/**
 * 엔티티의 [Instant] 필드를 기존 TEXT 컬럼 형식으로 읽고 쓴다.
 */
@Converter
class UtcInstantConverter : AttributeConverter<Instant, String> {
    override fun convertToDatabaseColumn(attribute: Instant?): String? =
        attribute?.let(UtcDatabaseTime::format)

    override fun convertToEntityAttribute(dbData: String?): Instant? =
        dbData?.let(UtcDatabaseTime::parse)
}
