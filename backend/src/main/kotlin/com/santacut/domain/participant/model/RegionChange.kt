package com.santacut.domain.participant.model

import com.santacut.domain.global.model.ForeignKeyColumns
import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 참가자가 지역을 바꾼 기록. 지역 변경 한도([RegionChangeLimit])를 세는 데만 쓴다.
 * 바꾼 지역은 남기지 않는다. 참가자를 지우면 함께 지워진다.
 */
@Entity
@Table(name = "region_changes", indexes = [Index(name = "region_changes_user", columnList = "user_id, changed_at")])
class RegionChange(
    id: String,
    userId: String,
    changedAt: Instant,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "user_id", nullable = false, columnDefinition = ForeignKeyColumns.PARTICIPANT)
    var userId: String = userId
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "changed_at", nullable = false)
    var changedAt: Instant = changedAt
        protected set

    companion object {
        fun record(participant: Participant, changedAt: Instant): RegionChange =
            RegionChange(UUID.randomUUID().toString(), participant.id, changedAt)
    }
}
