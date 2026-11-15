package com.santacut.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 등록한 참가자.
 */
@Entity
@Table(name = "participants")
class Participant(
    id: String,
    nickname: String,
    nicknameKey: String,
    regionCode: String,
    createdAt: Instant,
) {
    @Id
    @Column(name = "id")
    var id: String = id
        protected set

    @Column(name = "nickname", nullable = false)
    var nickname: String = nickname
        protected set

    @Column(name = "nickname_key", nullable = false, unique = true)
    var nicknameKey: String = nicknameKey
        protected set

    @Column(name = "region_code", nullable = false)
    var regionCode: String = regionCode
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = createdAt
        protected set

    fun participantId(): UUID = UUID.fromString(id)

    companion object {
        /** 데이터베이스가 닉네임 키 유일 제약 위반을 알릴 때 오류 메시지에 담는 이름(테이블.컬럼). */
        private const val NICKNAME_UNIQUE_CONSTRAINT = "participants.nickname_key"

        /**
         * 저장 오류 메시지가 닉네임 키 유일 제약 위반을 가리키는지 판별한다.
         * SQLite 드라이버는 유일 제약 위반을 확장 오류 코드로 알려 무결성 위반 예외로 바뀌지 않을 수 있어
         * 예외 종류 대신 메시지 안의 제약 이름으로 가려낸다.
         */
        fun isNicknameConflict(databaseMessage: String?): Boolean =
            databaseMessage?.contains(NICKNAME_UNIQUE_CONSTRAINT) == true

        fun register(nickname: Nickname, regionCode: String, registeredAt: Instant): Participant =
            Participant(UUID.randomUUID().toString(), nickname.value, nickname.key(), regionCode, registeredAt)
    }
}
